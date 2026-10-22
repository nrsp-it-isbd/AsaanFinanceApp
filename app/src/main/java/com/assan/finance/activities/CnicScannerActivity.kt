// Destination: app/src/main/java/com/assan/finance/activities/CnicScannerActivity.kt
package com.assan.finance.activities

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.assan.finance.BuildConfig
import com.assan.finance.R
import com.assan.finance.base.BaseActivity
import com.assan.finance.utils.CnicData
import com.assan.finance.network.assan.AssanRetrofitClient
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView
import android.graphics.Color
import com.assan.finance.utils.CommonFunctions
import com.assan.finance.utils.LocaleHelper

/**
 * Professional Pakistani-CNIC scanner.
 *
 * Flow:
 *  1) Live camera preview with a dashed "document scanner" style frame overlay
 *     ([ScannerOverlayView]) — only the marked area is later used for extraction.
 *  2) FRONT side is auto-captured only when we see: a valid CNIC number pattern
 *     + Urdu script + an English CNIC keyword (bilingual check => genuine Pakistani CNIC).
 *  3) User flips the card (auto 5s wait OR taps "I've Flipped the Card").
 *  4) BACK side is auto-captured only when Urdu script + an address/registrar keyword is seen.
 *  5) Captured photos are cropped to the marked frame (using CameraX ViewPort so the
 *     crop mapping is accurate), then sent to Gemini (online) or ML Kit (offline) to
 *     extract ONLY: Name, CNIC Number, Present Address, Permanent Address — with
 *     Urdu address text translated to English.
 *  6) Result is returned to the caller via Activity result extras; nothing else in the
 *     app (Applicant Details section, AI Voice Assistant, etc.) is touched by this screen.
 */
class CnicScannerActivity : BaseActivity() {

    companion object {
        private const val TAG = "CnicScanner"
        /** Trim the CNIC photos to the card's edges (background removed). false = old behaviour. */
        private const val AUTO_CROP_TO_CARD = true
        private val URDU_UNICODE_RANGE = 0x0600..0x06FF
    }

    // Views
    private lateinit var cameraPreview: PreviewView
    private lateinit var ivCapturedPreview: ImageView
    private lateinit var cnicFrameOverlay: View
    private lateinit var txtCameraHint: TextView
    private lateinit var scanLine: View
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnCancelScan: ImageView
    private lateinit var btnTorch: TextView
    private lateinit var processingOverlay: LinearLayout
    private lateinit var txtProcessingHint: TextView

    // State
    private val cnicData = CnicData()
    private var isScanningBack = false
    private var isTorchOn = false

    // Card focus + sharpness (like ID / fingerprint scanner SDKs)
    private val focusHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var blurryRetries = 0
    // CNIC number read from the front, to make sure the back belongs to the same card
    private var frontCnicDigits: String? = null
    private val refocusRunnable = object : Runnable {
        override fun run() {
            focusOnCard()
            focusHandler.postDelayed(this, 3000)
        }
    }
    private var frontImagePath: String? = null
    private var backImagePath: String? = null
    private var frontBitmap: Bitmap? = null
    private var backBitmap: Bitmap? = null


    // CameraX
    private var imageCapture: ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null
    private var scanAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cnic_scanner)

        cameraPreview = findViewById(R.id.cameraPreview)
        ivCapturedPreview = findViewById(R.id.ivCapturedPreview)
        cnicFrameOverlay = findViewById(R.id.cnicFrameOverlay)
        txtCameraHint = findViewById(R.id.txtCameraHint)
        scanLine = findViewById(R.id.scanLine)
        btnCapture = findViewById(R.id.btnCapture)
        btnCancelScan = findViewById(R.id.btnCancelScan)
        btnTorch = findViewById(R.id.btnTorch)
        processingOverlay = findViewById(R.id.processingOverlay)
        txtProcessingHint = findViewById(R.id.txtProcessingHint)

        btnCancelScan.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnTorch.setOnClickListener {
            toggleTorch()
        }

        btnCapture.setOnClickListener {
            if (!isScanningBack) {
                takePhoto(true)
            } else {
                takePhoto(false)
            }
        }

        // Wait for the overlay to be laid out so frameRect + ViewPort are available.
        cameraPreview.post {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 101)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.msg_camera_permission), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun toggleTorch() {
        val currentCamera = camera ?: return
        isTorchOn = !isTorchOn
        currentCamera.cameraControl.enableTorch(isTorchOn)
        btnTorch.text = if (isTorchOn) "💡 Flash: ON" else "💡 Flash: OFF"
    }

    // ==================== SCAN LINE ANIMATION ====================
    private fun startScanAnimation() {
        cnicFrameOverlay.post {
            if (cnicFrameOverlay.width == 0) return@post

            scanLine.visibility = View.VISIBLE

            scanAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, cnicFrameOverlay.height.toFloat() - scanLine.height.toFloat()).apply {
                duration = 2000L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun stopScanAnimation() {
        scanAnimator?.cancel()
        scanLine.visibility = View.GONE
    }

    // ==================== CAMERA SETUP ====================
    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

            // ~12 MP (4000x3000) still photos: the card comes out well above 300 DPI. Capped so very high
            // megapixel sensors do not run the phone out of memory.
            val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val bigPhotos = !am.isLowRamDevice && am.memoryClass >= 256
            val targetSize = if (bigPhotos) android.util.Size(4000, 3000) else android.util.Size(2592, 1944)
            val resolutionSelector = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                .setAspectRatioStrategy(androidx.camera.core.resolutionselector.AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    androidx.camera.core.resolutionselector.ResolutionStrategy(
                        targetSize,
                        androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(resolutionSelector)
                .setJpegQuality(100)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                val viewPort = cameraPreview.viewPort
                val cameraObj = if (viewPort != null) {
                    val useCaseGroup = UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(imageCapture!!)
                        .setViewPort(viewPort)
                        .build()
                    cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup)
                } else {
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                }
                this.camera = cameraObj
                cameraObj.cameraControl.enableTorch(isTorchOn)
                // Keep the focus on the card (centre of the frame) and let the user tap to refocus
                focusHandler.removeCallbacks(refocusRunnable)
                focusHandler.postDelayed(refocusRunnable, 500)
                cameraPreview.setOnTouchListener { v, ev ->
                    if (ev.action == android.view.MotionEvent.ACTION_UP) {
                        focusAt(ev.x, ev.y)
                        v.performClick()
                    }
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                Toast.makeText(this, R.string.auto_camera_initialization_fai_619, Toast.LENGTH_SHORT).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(isFront: Boolean) {
        val capture = imageCapture ?: return
        btnCapture.isEnabled = false
        btnCapture.text = getString(R.string.str_scanner_focusing)
        // Lock focus and exposure on the card first, then capture (fallback after 1.5 s if focus is slow)
        focusHandler.removeCallbacks(refocusRunnable)
        var fired = false
        val shoot = Runnable {
            if (!fired) {
                fired = true
                capturePhoto(capture, isFront)
            }
        }
        val focus = focusOnCard()
        if (focus == null) {
            shoot.run()
        } else {
            focus.addListener(shoot, ContextCompat.getMainExecutor(this))
            focusHandler.postDelayed(shoot, 1500)
        }
    }

    private fun capturePhoto(capture: ImageCapture, isFront: Boolean) {
        val captureLabel = getString(if (isFront) R.string.str_capture_front else R.string.str_capture_back)
        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    btnCapture.text = captureLabel
                    val bitmap = imageProxyToBitmap(image)
                    image.close()

                    if (bitmap == null) {
                        btnCapture.isEnabled = true
                        focusHandler.postDelayed(refocusRunnable, 300)
                        return
                    }
                    // Reject a blurry photo (moved hand / out of focus); after 2 retries accept it anyway
                    if (blurryRetries < 2 && isTooBlurry(bitmap)) {
                        blurryRetries++
                        Toast.makeText(this@CnicScannerActivity, R.string.str_scanner_not_clear, Toast.LENGTH_LONG).show()
                        btnCapture.isEnabled = true
                        focusHandler.postDelayed(refocusRunnable, 300)
                        return
                    }
                    blurryRetries = 0
                    validateSideThenProcess(bitmap, isFront)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    btnCapture.text = captureLabel
                    btnCapture.isEnabled = true
                }
            }
        )
    }

    private fun processCapturedBitmap(bitmap: Bitmap, isFront: Boolean) {
        // Freeze screen by showing captured image
        ivCapturedPreview.setImageBitmap(bitmap)
        ivCapturedPreview.visibility = View.VISIBLE

        // Start scanning animation
        startScanAnimation()

        if (isFront) {
            txtCameraHint.text = getString(R.string.str_scanning_front_side)
            txtCameraHint.setTextColor(Color.parseColor("#10B981"))
        } else {
            txtCameraHint.text = getString(R.string.str_scanning_back_side)
            txtCameraHint.setTextColor(Color.parseColor("#10B981"))
        }

        // Save in background while animating
        val savedPath = saveBitmapToFile(bitmap, if (isFront) "cnic_front" else "cnic_back")

        lifecycleScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(1500)

            stopScanAnimation()
            ivCapturedPreview.visibility = View.GONE
            btnCapture.isEnabled = true

            if (isFront) {
                frontBitmap = bitmap
                frontImagePath = savedPath
                switchToBackScanning()
            } else {
                backBitmap = bitmap
                backImagePath = savedPath
                showProcessingOverlay()
                extractCnicData()
            }
        }
    }

    private fun switchToBackScanning() {
        isScanningBack = true
        runOnUiThread {
            txtCameraHint.text = getString(R.string.str_front_captured_align_back)
            txtCameraHint.setTextColor(Color.BLACK) // was WHITE: invisible on the white page
            txtCameraHint.textSize = 14f
            btnCapture.text = getString(R.string.str_capture_back)
        }
    }

    private fun showProcessingOverlay() {
        runOnUiThread {
            processingOverlay.visibility = View.VISIBLE
            txtProcessingHint.text = getString(R.string.str_extracting_cnic_data)
        }
    }

    private fun extractCnicData() {
        val front = frontBitmap
        val back = backBitmap
        if (front == null || back == null) {
            val isUrdu = LocaleHelper.getLanguage(this) == "ur"
            val title = if (isUrdu) "تصاویر نامکمل ہیں" else "Images Incomplete"
            val msg = if (isUrdu) "شناختی کارڈ کے دونوں اطراف کی تصاویر حاصل نہیں ہو سکیں۔ براہ کرم دوبارہ اسکین کریں۔" else "Both sides of the CNIC were not captured properly. Please try again."
            val btn = if (isUrdu) "دوبارہ اسکین کریں" else "Try Again"
            CommonFunctions.showPremiumFeedbackDialog(this, "warning", title, msg, btn) {
                resetToFrontScanning()
            }
            return
        }

        if (isNetworkAvailable()) {
            extractWithGemini(front, back)
        } else {
            showScannerErrorDialog(Exception("network_connection_required"))
        }
    }

    private fun onExtractionError(e: Exception) {
        runOnUiThread {
            txtProcessingHint.text = getString(R.string.str_error_exclamation)
            processingOverlay.visibility = View.GONE
            stopScanAnimation()
            android.util.Log.e("CnicScanner", "Error processing CNIC", e)
            showScannerErrorDialog(e)
        }
    }

    // ==================== ONLINE EXTRACTION (Backend Gemini OCR) ====================
    private fun bitmapToMultipart(bitmap: Bitmap, partName: String, filename: String): MultipartBody.Part {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = byteArray.toRequestBody(mediaType)
        return MultipartBody.Part.createFormData(partName, filename, requestBody)
    }

    private fun parseAndApplyOcrData(json: com.google.gson.JsonObject) {
        fun getString(vararg keys: String): String {
            for (k in keys) {
                if (json.has(k) && !json.get(k).isJsonNull) {
                    val str = json.get(k).asString.trim()
                    if (str.isNotEmpty()) return str
                }
            }
            return ""
        }

        val cnic = getString("cnicNumber", "cnic", "cnic_number", "CNIC")
        if (cnic.isNotEmpty()) cnicData.cnicNumber = cnic

        val nameEn = getString("nameEn", "name", "full_name", "fullName", "NAME")
        if (nameEn.isNotEmpty()) cnicData.nameEn = nameEn

        val nameUr = getString("nameUr", "name_ur", "nameUrdu")
        if (nameUr.isNotEmpty()) cnicData.nameUr = nameUr

        val fatherNameEn = getString("fatherNameEn", "fatherName", "father_name", "husbandName")
        if (fatherNameEn.isNotEmpty()) cnicData.fatherNameEn = fatherNameEn

        val fatherNameUr = getString("fatherNameUr", "father_name_ur", "fatherNameUrdu")
        if (fatherNameUr.isNotEmpty()) cnicData.fatherNameUr = fatherNameUr

        val rawDob = getString("dateOfBirth", "dob", "birth_date")
        if (rawDob.isNotEmpty()) {
            val formatted = CommonFunctions.formatDateOfBirth(rawDob)
            cnicData.dateOfBirth = if (formatted.isNotEmpty()) formatted else rawDob
        }

        val issueDate = getString("issueDate", "issue_date")
        if (issueDate.isNotEmpty()) cnicData.issueDate = issueDate

        val expiryDate = getString("expiryDate", "expiry_date")
        if (expiryDate.isNotEmpty()) cnicData.expiryDate = expiryDate

        val gender = getString("gender", "Gender")
        if (gender.isNotEmpty()) cnicData.gender = gender

        val country = getString("country", "country_of_stay")
        if (country.isNotEmpty()) cnicData.country = country

        val presentAddress = getString("presentAddress", "present_address", "current_address")
        if (presentAddress.isNotEmpty()) cnicData.presentAddress = presentAddress

        val permanentAddress = getString("permanentAddress", "permanent_address")
        if (permanentAddress.isNotEmpty()) cnicData.permanentAddress = permanentAddress
    }

    private fun extractWithGemini(front: Bitmap, back: Bitmap) {
        runOnUiThread {
            processingOverlay.visibility = View.VISIBLE
            txtProcessingHint.text = getString(R.string.str_extracting_cnic_data)
        }

        com.assan.finance.utils.CnicExtractionHelper.extractFromBitmaps(
            this@CnicScannerActivity,
            front,
            back,
            onResult = { jsonObject ->
                if (jsonObject != null) {
                    val cnic = jsonObject.optString("cnicNumber", "")
                    if (cnic.isNotEmpty()) cnicData.cnicNumber = cnic

                    val nameEn = jsonObject.optString("nameEn", "")
                    if (nameEn.isNotEmpty()) cnicData.nameEn = nameEn

                    val fatherNameEn = jsonObject.optString("fatherNameEn", "")
                    if (fatherNameEn.isNotEmpty()) cnicData.fatherNameEn = fatherNameEn

                    val rawDob = jsonObject.optString("dateOfBirth", "")
                    if (rawDob.isNotEmpty()) {
                        val formatted = CommonFunctions.formatDateOfBirth(rawDob)
                        cnicData.dateOfBirth = if (formatted.isNotEmpty()) formatted else rawDob
                    }

                    val issueDate = jsonObject.optString("issueDate", "")
                    if (issueDate.isNotEmpty()) {
                        val formatted = CommonFunctions.formatDateOfBirth(issueDate)
                        cnicData.issueDate = if (formatted.isNotEmpty()) formatted else issueDate
                    }

                    val expiryDate = jsonObject.optString("expiryDate", "")
                    if (expiryDate.isNotEmpty()) {
                        val formatted = CommonFunctions.formatDateOfBirth(expiryDate)
                        cnicData.expiryDate = if (formatted.isNotEmpty()) formatted else expiryDate
                    }

                    val gender = jsonObject.optString("gender", "")
                    if (gender.isNotEmpty()) cnicData.gender = gender

                    val presentAddress = jsonObject.optString("presentAddress", "")
                    if (presentAddress.isNotEmpty()) cnicData.presentAddress = presentAddress

                    val permanentAddress = jsonObject.optString("permanentAddress", "")
                    if (permanentAddress.isNotEmpty()) cnicData.permanentAddress = permanentAddress

                    if (AUTO_CROP_TO_CARD) cropSavedImagesToCard(front, back)

                    processingOverlay.visibility = View.GONE
                    returnResultToLoanForm()
                } else {
                    processingOverlay.visibility = View.GONE
                    stopScanAnimation()
                    showScannerErrorDialog(Exception("cnic_data_not_readable"))
                }
            },
            onError = { errorMsg ->
                // FIX: previously any failure showed "CNIC images captured successfully!" and returned
                // to the form with empty data, so none of the error dialogs below could ever appear.
                processingOverlay.visibility = View.GONE
                stopScanAnimation()
                showScannerErrorDialog(Exception(errorMsg))
            }
        )
    }

    private fun showScannerErrorDialog(error: Throwable?) {
        if (isFinishing || isDestroyed) return
        val isUrdu = LocaleHelper.getLanguage(this) == "ur"
        val errorMsg = (error?.message ?: "").lowercase(Locale.ROOT)

        val type: String
        val title: String
        val message: String
        var buttonText: String
        // Server-side problems (quota, key, 502/503): rescanning won't help, so keep the
        // captured photos and let the user type the details in the form.
        var continueManually = false

        when {
            // 0. Backend AI Server Model Issue (e.g. 502 / Gemini API failed / model not available)
            errorMsg.contains("502") || errorMsg.contains("gemini api failed") || errorMsg.contains("is no longer available") || (errorMsg.contains("model") && errorMsg.contains("gemini")) -> {
                type = "error"
                title = if (isUrdu) "اے آئی سرور کا مسئلہ" else "AI Server Configuration Issue"
                message = if (isUrdu)
                    "بیک اینڈ سرور پر اے آئی ماڈل کی کنفیگریشن میں مسئلہ ہے۔ سرور پر ماڈل کا نام اپڈیٹ ہونا باقی ہے۔ براہ کرم بیک اینڈ ٹیم سے رابطہ کریں۔"
                else
                    "The backend server encountered an AI model configuration error (502). Please contact the backend team to update the model in server code."
                buttonText = if (isUrdu) "ٹھیک ہے" else "Okay"
                continueManually = true
            }

            // 1. Quota / Credit Exceeded
            errorMsg.contains("429") || errorMsg.contains("quota") || errorMsg.contains("exhausted") || errorMsg.contains("resourceexhausted") -> {
                type = "warning"
                title = if (isUrdu) "اسکیننگ کوٹہ ختم ہو گیا" else "Scanning Quota Exceeded"
                message = if (isUrdu)
                    "شناختی کارڈ اے آئی اسکیننگ سروس کا یومیہ کوٹہ ختم ہو چکا ہے۔ براہ کرم کچھ دیر بعد کوشش کریں یا ایڈمن سے رابطہ کریں۔"
                else
                    "The daily AI scanning service quota has been exceeded. Please try again later or contact technical support."
                buttonText = if (isUrdu) "ٹھیک ہے" else "Okay"
                continueManually = true
            }

            // 2. API Key Expired or Invalid
            errorMsg.contains("api_key") || errorMsg.contains("api key") || errorMsg.contains("401") || errorMsg.contains("403") || errorMsg.contains("unauthorized") || errorMsg.contains("permission_denied") -> {
                type = "error"
                title = if (isUrdu) "سروس کی تصدیق کا مسئلہ" else "Service Key Error"
                message = if (isUrdu)
                    "اے آئی اسکیننگ سروس کی کلید (API Key) درست نہیں ہے یا ایکسپائر ہو گئی ہے۔ براہ کرم ایڈمن سے رابطہ کریں۔"
                else
                    "The AI service API key is invalid or expired. Please contact technical support."
                buttonText = if (isUrdu) "ٹھیک ہے" else "Okay"
                continueManually = true
            }

            // 3. Server Busy / 503 / High Demand
            errorMsg.contains("503") || errorMsg.contains("high demand") || errorMsg.contains("unavailable") || errorMsg.contains("overloaded") || errorMsg.contains("heavily loaded") -> {
                type = "warning"
                title = if (isUrdu) "سرور پر شدید رش ہے" else "AI Servers Busy"
                message = if (isUrdu)
                    "اے آئی سرورز پر اس وقت بہت زیادہ ٹریفک ہے۔ براہ کرم چند لمحے بعد دوبارہ کوشش کریں۔"
                else
                    "All AI servers are currently experiencing heavy traffic. Please wait a moment and try again."
                buttonText = if (isUrdu) "ٹھیک ہے" else "Okay"
                continueManually = true
            }

            // 4. Network / Connection issue
            errorMsg.contains("network") || errorMsg.contains("timeout") || errorMsg.contains("connect") || errorMsg.contains("unknownhost") -> {
                type = "warning"
                title = if (isUrdu) "انٹرنیٹ کنکشن کا مسئلہ" else "Connection Problem"
                message = if (isUrdu)
                    "انٹرنیٹ کنکشن کمزور ہے یا رابطہ منقطع ہو گیا۔ براہ کرم انٹرنیٹ چیک کر کے دوبارہ اسکین کریں۔"
                else
                    "Internet connection is unstable or lost. Please verify your connection and try scanning again."
                buttonText = if (isUrdu) "دوبارہ کوشش کریں" else "Retry"
            }

            // 5. The picture is not a CNIC at all (another card, paper, screen...)
            errorMsg.contains("not a cnic") -> {
                type = "warning"
                title = if (isUrdu) "یہ شناختی کارڈ نہیں ہے" else "Not a CNIC"
                message = if (isUrdu)
                    "یہ تصویر پاکستانی شناختی کارڈ کی نہیں لگتی۔ براہ کرم اپنے اصل شناختی کارڈ کا سامنے اور پیچھے والا حصہ اسکین کریں۔"
                else
                    "This picture does not look like a Pakistani CNIC. Please scan the front and back of your original CNIC."
                buttonText = if (isUrdu) "دوبارہ اسکین کریں" else "Scan Again"
            }

            // 6. Default / Blurry / Could not read card
            else -> {
                type = "warning"
                title = if (isUrdu) "شناختی کارڈ واضح نہیں ہے" else "Could Not Read CNIC"
                message = if (isUrdu)
                    "شناختی کارڈ کی تفصیلات واضح نظر نہیں آ رہیں۔ براہ کرم کارڈ کو مناسب روشنی میں، بغیر چمک (Glare) کے فریم کے اندر رکھ کر دوبارہ اسکین کریں۔"
                else
                    "Unable to read CNIC details clearly. Please ensure proper lighting, avoid glare or blur, and align the card inside the frame."
                buttonText = if (isUrdu) "دوبارہ اسکین کریں" else "Scan Again"
            }
        }

        val hasBothImages = !frontImagePath.isNullOrEmpty() && !backImagePath.isNullOrEmpty()
        if (continueManually && hasBothImages) {
            buttonText = if (isUrdu) "تفصیلات خود درج کریں" else "Enter Details Manually"
        }

        CommonFunctions.showPremiumFeedbackDialog(
            this,
            type,
            title,
            message,
            buttonText
        ) {
            if (continueManually && hasBothImages) {
                returnResultToLoanForm(extractionFailed = true)
            } else {
                resetToFrontScanning()
            }
        }
    }

    private fun resetToFrontScanning() {
        isScanningBack = false
        frontCnicDigits = null
        frontBitmap = null
        backBitmap = null
        frontImagePath = null
        backImagePath = null
        processingOverlay.visibility = View.GONE
        txtCameraHint.text = getString(R.string.str_align_front_inside_frame)
        txtCameraHint.setTextColor(Color.BLACK)
        btnCapture.text = getString(R.string.str_capture_front)
        ivCapturedPreview.visibility = View.GONE
        btnCapture.isEnabled = true
        stopScanAnimation()
    }

    // ==================== RESULT HANDLING ====================
    // Only the 4 requested fields are returned: Name, CNIC, Present Address, Permanent Address.
    // (Plus image paths, needed by the loan form to mark front/back as scanned.)
    private fun returnResultToLoanForm(extractionFailed: Boolean = false) {
        try {
            val resultIntent = Intent()
            resultIntent.putExtra("EXTRACTION_FAILED", extractionFailed)
            resultIntent.putExtra("FRONT_IMAGE_PATH", frontImagePath ?: "")
            resultIntent.putExtra("BACK_IMAGE_PATH", backImagePath ?: "")
            resultIntent.putExtra("CNIC_NUMBER", cnicData.cnicNumber)
            resultIntent.putExtra("NAME_EN", cnicData.nameEn)
            resultIntent.putExtra("NAME_UR", cnicData.nameUr)
            resultIntent.putExtra("FATHER_NAME_EN", cnicData.fatherNameEn)
            resultIntent.putExtra("FATHER_NAME_UR", cnicData.fatherNameUr)
            resultIntent.putExtra("DATE_OF_BIRTH", cnicData.dateOfBirth)
            resultIntent.putExtra("ISSUE_DATE", cnicData.issueDate)
            resultIntent.putExtra("EXPIRY_DATE", cnicData.expiryDate)
            resultIntent.putExtra("GENDER", cnicData.gender)
            resultIntent.putExtra("COUNTRY", cnicData.country)
            resultIntent.putExtra("PRESENT_ADDRESS", cnicData.presentAddress)
            resultIntent.putExtra("PERMANENT_ADDRESS", cnicData.permanentAddress)

            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to return CNIC data", e)
            android.util.Log.e("CnicScanner", "Failed to return data", e)
                Toast.makeText(this, R.string.auto_failed_to_return_data_e_m_622, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ==================== UTILITY METHODS ====================
    // FIX: the full camera photo (hand, laptop, table) was saved and uploaded. The captured JPEG is
    // never cropped by CameraX for in-memory captures — the ViewPort crop only comes as
    // image.cropRect — so it is applied here, then the result is trimmed to the frame's shape.
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

            // 1. ViewPort crop (un-rotated sensor coordinates, scaled if the JPEG size differs)
            val crop = image.cropRect
            if (image.width > 0 && image.height > 0 && !crop.isEmpty) {
                val sx = bitmap.width.toFloat() / image.width
                val sy = bitmap.height.toFloat() / image.height
                val left = (crop.left * sx).toInt().coerceIn(0, bitmap.width - 1)
                val top = (crop.top * sy).toInt().coerceIn(0, bitmap.height - 1)
                val width = (crop.width() * sx).toInt().coerceIn(1, bitmap.width - left)
                val height = (crop.height() * sy).toInt().coerceIn(1, bitmap.height - top)
                if (width < bitmap.width || height < bitmap.height) {
                    bitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                }
            }

            // 2. Rotate upright
            val rotation = image.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            // 3. Keep only the frame area (no-op when the ViewPort crop already matched it)
            cropToFrameAspect(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert ImageProxy to Bitmap", e)
            null
        }
    }

    /**
     * Centre-crops [bitmap] to the aspect ratio of the on-screen CNIC frame. PreviewView shows the
     * centre of the image (FILL_CENTER), so this is exactly the area the user saw inside the border.
     */
    private fun cropToFrameAspect(bitmap: Bitmap): Bitmap {
        val frameW = cnicFrameOverlay.width
        val frameH = cnicFrameOverlay.height
        if (frameW <= 0 || frameH <= 0) return bitmap

        val frameAspect = frameW.toFloat() / frameH
        val bmpAspect = bitmap.width.toFloat() / bitmap.height
        if (Math.abs(bmpAspect - frameAspect) < 0.01f) return bitmap

        return if (bmpAspect > frameAspect) {
            // Too wide: trim left/right
            val newW = (bitmap.height * frameAspect).toInt().coerceIn(1, bitmap.width)
            Bitmap.createBitmap(bitmap, (bitmap.width - newW) / 2, 0, newW, bitmap.height)
        } else {
            // Too tall: trim top/bottom
            val newH = (bitmap.width / frameAspect).toInt().coerceIn(1, bitmap.height)
            Bitmap.createBitmap(bitmap, 0, (bitmap.height - newH) / 2, bitmap.width, newH)
        }
    }

    /**
     * Background removal: trims the saved front/back photos to the card's edges (on the phone), so the
     * portal shows only the card. Keeps the frame-cropped photo when no clear card edge is found.
     * Set AUTO_CROP_TO_CARD = false to switch this off.
     */
    private fun cropSavedImagesToCard(front: Bitmap, back: Bitmap) {
        val helper = com.assan.finance.utils.CnicExtractionHelper
        helper.trimToCardEdges(front)?.let { cropped ->
            saveBitmapToFile(cropped, "cnic_front_card")?.let { frontImagePath = it }
        }
        helper.trimToCardEdges(back)?.let { cropped ->
            saveBitmapToFile(cropped, "cnic_back_card")?.let { backImagePath = it }
        }
    }

    /**
     * Only a Pakistani CNIC may be scanned: read the text on the photo (on the phone, ML Kit) and stop the
     * user right at this step when the front / back is wrong, before anything is sent to the AI.
     */
    private fun validateSideThenProcess(bitmap: Bitmap, isFront: Boolean) {
        btnCapture.isEnabled = false
        btnCapture.text = getString(R.string.str_scanner_checking)
        val w = 1600
        val small = if (bitmap.width > w)
            Bitmap.createScaledBitmap(bitmap, w, (bitmap.height * w.toFloat() / bitmap.width).toInt(), true) else bitmap
        try {
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(small, 0)
            com.google.mlkit.vision.text.TextRecognition
                .getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnCompleteListener { textTask ->
                    // Text reader not available on this phone: accept; the AI check at the end still rejects non-CNICs
                    if (!textTask.isSuccessful) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "SideCheck text reader failed: ${textTask.exception?.message}")
                        handleSideCheck(bitmap, isFront, "OK")
                        return@addOnCompleteListener
                    }
                    val text = textTask.result?.text ?: ""
                    try {
                        com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
                            .process(image)
                            .addOnCompleteListener { codeTask ->
                                // null = barcode reader not available, so only the text is used
                                val hasCode: Boolean? = if (codeTask.isSuccessful) !codeTask.result.isNullOrEmpty() else null
                                val verdict = checkCnicSide(text, hasCode, isFront)
                                if (BuildConfig.DEBUG) {
                                    // no card text in the log: it contains personal data
                                    Log.d(TAG, "SideCheck front=$isFront verdict=$verdict code=$hasCode formats=${codeTask.result?.map { it.format }}")
                                }
                                handleSideCheck(bitmap, isFront, verdict)
                            }
                    } catch (e: Exception) {
                        handleSideCheck(bitmap, isFront, checkCnicSide(text, null, isFront))
                    }
                }
        } catch (e: Exception) {
            handleSideCheck(bitmap, isFront, "OK")
        }
    }

    /**
     * Works for the smart CNIC / NICOP and the old (Urdu) CNIC. The phone's text reader only reads English
     * and digits, so the card is recognised by:
     *  - front: the 13-digit CNIC number (old card) or the English "National Identity Card" text (smart card),
     *    and NO barcode / QR code on it;
     *  - back: a QR code (smart card) or a barcode (old card), or "Registrar General of Pakistan", or (old card,
     *    when the barcode cannot be read) the CNIC number with two dates (issue and expiry; the front has one).
     * Returns "OK", "NOT_FRONT", "FRONT_AGAIN", "NOT_BACK" or "DIFFERENT_CARD".
     */
    private fun checkCnicSide(rawText: String, hasCode: Boolean?, isFront: Boolean): String {
        val compact = rawText.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
        val cnic = Regex("(?<!\\d)(\\d{5})\\s*[-–.]?\\s*(\\d{7})\\s*[-–.]?\\s*(\\d)(?!\\d)").find(rawText)
            ?.let { it.groupValues[1] + it.groupValues[2] + it.groupValues[3] }
        val smartFrontText = compact.contains("IDENTITYCARD") ||
                (compact.contains("PAKISTAN") && (compact.contains("IDENTITYNUMBER") || compact.contains("DATEOFBIRTH")
                        || compact.contains("FATHERNAME") || compact.contains("HUSBANDNAME")))
        val backText = compact.contains("REGISTRARGENERAL")
        // Old CNIC: the front has one date (birth), the back has two (issue and expiry)
        val dateCount = Regex("(?<!\\d)\\d{2}\\s*[/.-]\\s*\\d{2}\\s*[/.-]\\s*\\d{4}(?!\\d)").findAll(rawText).count()
        val oldBackByDates = cnic != null && dateCount >= 2 && !smartFrontText
        return if (isFront) {
            when {
                hasCode == true || backText || oldBackByDates -> "NOT_FRONT"   // that is a back side
                smartFrontText || cnic != null -> {
                    frontCnicDigits = cnic
                    "OK"
                }
                else -> "NOT_FRONT"
            }
        } else {
            val front = frontCnicDigits
            val sameAsFront = cnic != null && front != null && cnic.indices.count { cnic[it] != front[it] } <= 1
            val otherCard = cnic != null && front != null && !sameAsFront
            when {
                smartFrontText -> "FRONT_AGAIN"
                hasCode == true || backText || oldBackByDates -> if (otherCard) "DIFFERENT_CARD" else "OK"
                // barcode reader not available on this phone: fall back to the CNIC number
                hasCode == null && cnic != null -> if (otherCard) "DIFFERENT_CARD" else "OK"
                // same number but no barcode / QR: the (old) front was scanned again
                sameAsFront -> "FRONT_AGAIN"
                else -> "NOT_BACK"
            }
        }
    }

    private fun handleSideCheck(bitmap: Bitmap, isFront: Boolean, result: String) {
        if (isFinishing || isDestroyed) return
        btnCapture.text = getString(if (isFront) R.string.str_capture_front else R.string.str_capture_back)
        if (result == "OK") {
            processCapturedBitmap(bitmap, isFront)
            focusHandler.postDelayed(refocusRunnable, 1800)
            return
        }
        val isUrdu = LocaleHelper.getLanguage(this) == "ur"
        val (title, message) = when (result) {
            "NOT_FRONT" -> if (isUrdu) Pair("یہ شناختی کارڈ کا سامنے والا حصہ نہیں",
                "یہ پاکستانی شناختی کارڈ کا سامنے والا حصہ نہیں ہے یا واضح نہیں۔ اپنے اصل شناختی کارڈ کا سامنے والا حصہ (جس پر آپ کی تصویر ہے) فریم میں رکھ کر دوبارہ تصویر کھینچیں۔")
            else Pair("Not the CNIC Front",
                "This is not the front side of a Pakistani CNIC, or it is not clear. Place the FRONT of your original CNIC (with your photo) inside the frame and capture again.")
            "FRONT_AGAIN" -> if (isUrdu) Pair("پچھلا حصہ اسکین کریں",
                "آپ نے دوبارہ سامنے والا حصہ اسکین کیا ہے۔ کارڈ پلٹ کر پچھلا حصہ (جس پر QR کوڈ یا بارکوڈ ہے) اسکین کریں۔")
            else Pair("Back Side Needed", "You scanned the front side again. Turn the card over and scan the BACK side (with the QR code or barcode).")
            "DIFFERENT_CARD" -> if (isUrdu) Pair("مختلف کارڈ",
                "یہ پچھلا حصہ کسی دوسرے شناختی کارڈ کا ہے۔ براہ کرم اسی شناختی کارڈ کا پچھلا حصہ اسکین کریں۔")
            else Pair("Different Card", "This back side belongs to a different CNIC. Please scan the back of the same CNIC.")
            else -> if (isUrdu) Pair("یہ شناختی کارڈ کا پچھلا حصہ نہیں",
                "یہ پاکستانی شناختی کارڈ کا پچھلا حصہ نہیں ہے یا واضح نہیں۔ اسی شناختی کارڈ کا پچھلا حصہ (جس پر QR کوڈ یا بارکوڈ ہے) اسکین کریں۔")
            else Pair("Not the CNIC Back",
                "This is not the back side of a Pakistani CNIC, or it is not clear. Scan the BACK of the same CNIC (with the QR code or barcode).")
        }
        btnCapture.isEnabled = true
        CommonFunctions.showPremiumFeedbackDialog(this, "warning", title, message,
            if (isUrdu) "دوبارہ اسکین کریں" else "Scan Again") {
            focusHandler.postDelayed(refocusRunnable, 300)
        }
    }

    /** Centre of the on-screen CNIC frame, in PreviewView coordinates. */
    private fun cardCenterInPreview(): Pair<Float, Float> {
        val p = IntArray(2)
        val o = IntArray(2)
        cameraPreview.getLocationOnScreen(p)
        cnicFrameOverlay.getLocationOnScreen(o)
        return Pair(o[0] - p[0] + cnicFrameOverlay.width / 2f, o[1] - p[1] + cnicFrameOverlay.height / 2f)
    }

    private fun focusOnCard(): com.google.common.util.concurrent.ListenableFuture<FocusMeteringResult>? {
        if (cnicFrameOverlay.width == 0) return null
        val (x, y) = cardCenterInPreview()
        return focusAt(x, y)
    }

    /** Auto focus + auto exposure on a region around (x, y) of the preview. */
    private fun focusAt(x: Float, y: Float): com.google.common.util.concurrent.ListenableFuture<FocusMeteringResult>? {
        val cam = camera ?: return null
        return try {
            val point = cameraPreview.meteringPointFactory.createPoint(x, y, 0.3f)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(4, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.w(TAG, "Focus failed: ${e.message}")
            null
        }
    }

    /**
     * Sharpness = variance of the Laplacian on an 800 px wide grey copy. Sharp CNIC photos score about
     * 250 (back) to 450 (front); a slightly blurred photo drops below 60.
     */
    private fun isTooBlurry(bitmap: Bitmap): Boolean {
        return try {
            val w = 800
            val h = (bitmap.height * w.toFloat() / bitmap.width).toInt().coerceAtLeast(3)
            val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
            val px = IntArray(w * h)
            small.getPixels(px, 0, w, 0, 0, w, h)
            if (small != bitmap) small.recycle()
            val g = FloatArray(w * h) { i ->
                val c = px[i]
                ((c shr 16) and 0xFF) * 0.299f + ((c shr 8) and 0xFF) * 0.587f + (c and 0xFF) * 0.114f
            }
            var sum = 0.0
            var sumSq = 0.0
            var n = 0
            for (y in 1 until h - 1) for (x in 1 until w - 1) {
                val i = y * w + x
                val lap = (g[i - 1] + g[i + 1] + g[i - w] + g[i + w] - 4 * g[i]).toDouble()
                sum += lap; sumSq += lap * lap; n++
            }
            val mean = sum / n
            val variance = sumSq / n - mean * mean
            Log.d(TAG, "Capture sharpness: ${variance.toInt()}")
            variance < 60
        } catch (e: Exception) {
            false
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap, prefix: String): String? {
        return try {
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "${prefix}_${timestamp}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            // Record the real resolution: a CNIC is 85.6 mm (3.37 in) wide, so DPI = long side / 3.37
            try {
                val dpi = (maxOf(bitmap.width, bitmap.height) / 3.37).toInt().coerceAtLeast(1)
                androidx.exifinterface.media.ExifInterface(file.absolutePath).apply {
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_X_RESOLUTION, "$dpi/1")
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_Y_RESOLUTION, "$dpi/1")
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_RESOLUTION_UNIT, "2")
                    saveAttributes()
                }
            } catch (ignored: Exception) {
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bitmap", e)
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        focusHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
        scanAnimator?.cancel()
    }
}