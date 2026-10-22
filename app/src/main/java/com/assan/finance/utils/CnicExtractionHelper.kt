package com.assan.finance.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.assan.finance.network.assan.AssanRetrofitClient
import com.assan.finance.sharedPreferences.SharedManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object CnicExtractionHelper {

    private const val TAG = "CnicExtractionHelper"
    // SECURITY TODO: an API key inside the APK can be extracted by anyone who downloads the app.
    // Move this call to the backend (/api/Ai/gemini-ocr already exists) and delete the key from here.
    // Key comes from local.properties or the CI secret, never from source control
    private val GEMINI_API_KEY = com.assan.finance.BuildConfig.GEMINI_API_KEY

    @Volatile
    private var lastAiError: String = ""

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts CNIC details from front and optional back image Uris.
     */
    fun extractWithGemini(
        context: Context,
        frontUri: Uri,
        backUri: Uri?,
        onResult: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val frontBitmap = getBitmapFromUri(context, frontUri)
        val backBitmap = if (backUri != null) getBitmapFromUri(context, backUri) else null

        if (frontBitmap == null) {
            onError("Could not load CNIC front image. Please try selecting the picture again.")
            return
        }

        extractFromBitmaps(context, frontBitmap, backBitmap, onResult, onError)
    }

    /**
     * Extracts CNIC details directly from Bitmaps (used by camera scanner and gallery upload).
     */
    fun extractFromBitmaps(
        context: Context,
        frontBitmap: Bitmap,
        backBitmap: Bitmap?,
        onResult: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            lastAiError = ""
            val finalResult = JSONObject()

            // =========================================================================
            // LAYER 1: Direct Google Gemini Vision API (Primary, Ultra-Fast & Accurate)
            // =========================================================================
            var geminiSuccess = false
            try {
                Log.d(TAG, "Attempting Layer 1: Direct Gemini Vision OCR...")
                val geminiJson = callDirectGeminiVision(frontBitmap, backBitmap)
                if (geminiJson != null && geminiJson.optString("isCnic", "").equals("false", ignoreCase = true)) {
                    withContext(Dispatchers.Main) { onError(NOT_A_CNIC_MESSAGE) }
                    return@launch
                }
                if (geminiJson != null) {
                    mergeJsonData(finalResult, geminiJson)
                    if (hasSufficientData(finalResult)) {
                        geminiSuccess = true
                        Log.d(TAG, "Direct Gemini OCR succeeded with data: $finalResult")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Layer 1 (Direct Gemini) encountered an error: ${e.message}")
            }

            // =========================================================================
            // LAYER 2: Backend API Fallback (/api/Ai/gemini-ocr)
            // =========================================================================
            if (!geminiSuccess) {
                try {
                    Log.d(TAG, "Attempting Layer 2: Backend Gemini OCR Fallback...")
                    val backendFrontPart = bitmapToMultipart(frontBitmap, "image", "front.jpg")
                    val sideFront = "front".toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = AssanRetrofitClient.apiService.processGeminiOcr(backendFrontPart, sideFront)
                    if (!response.isSuccessful) lastAiError = "HTTP ${response.code()} backend ocr"
                    if (response.isSuccessful && response.body() != null) {
                        val root = response.body()!!
                        val data = if (root.has("data") && root.get("data").isJsonObject) {
                            root.getAsJsonObject("data")
                        } else if (root.has("result") && root.get("result").isJsonObject) {
                            root.getAsJsonObject("result")
                        } else {
                            root
                        }

                        copyGsonField(finalResult, data, "cnicNumber", "cnicNumber", "cnic", "cnic_number", "CNIC")
                        copyGsonField(finalResult, data, "nameEn", "nameEn", "name", "full_name", "fullName", "NAME")
                        copyGsonField(finalResult, data, "fatherNameEn", "fatherNameEn", "fatherName", "father_name", "husbandName")
                        copyGsonField(finalResult, data, "dateOfBirth", "dateOfBirth", "dob", "birth_date")
                        copyGsonField(finalResult, data, "issueDate", "issueDate", "issue_date")
                        copyGsonField(finalResult, data, "expiryDate", "expiryDate", "expiry_date")
                        copyGsonField(finalResult, data, "gender", "gender", "Gender")
                        copyGsonField(finalResult, data, "presentAddress", "presentAddress", "present_address", "current_address")
                        copyGsonField(finalResult, data, "permanentAddress", "permanentAddress", "permanent_address")

                        // Process Back side via Backend if available
                        if (backBitmap != null) {
                            try {
                                val backendBackPart = bitmapToMultipart(backBitmap, "image", "back.jpg")
                                val sideBack = "back".toRequestBody("text/plain".toMediaTypeOrNull())
                                val backResponse = AssanRetrofitClient.apiService.processGeminiOcr(backendBackPart, sideBack)
                                if (backResponse.isSuccessful && backResponse.body() != null) {
                                    val bRoot = backResponse.body()!!
                                    val bData = if (bRoot.has("data") && bRoot.get("data").isJsonObject) {
                                        bRoot.getAsJsonObject("data")
                                    } else if (bRoot.has("result") && bRoot.get("result").isJsonObject) {
                                        bRoot.getAsJsonObject("result")
                                    } else {
                                        bRoot
                                    }
                                    copyGsonFieldIfEmpty(finalResult, bData, "presentAddress", "presentAddress", "present_address", "current_address", "address")
                                    copyGsonFieldIfEmpty(finalResult, bData, "permanentAddress", "permanentAddress", "permanent_address", "address")
                                }
                            } catch (be: Exception) {
                                Log.w(TAG, "Backend OCR back side warning: ${be.message}")
                            }
                        }

                        if (hasSufficientData(finalResult)) {
                            geminiSuccess = true
                            Log.d(TAG, "Backend OCR succeeded with data: $finalResult")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Layer 2 (Backend OCR) encountered an error: ${e.message}")
                    lastAiError = "network ${e.javaClass.simpleName} ${e.message ?: ""}"
                }
            }

            // =========================================================================
            // LAYER 3: On-Device Google ML Kit Text Recognition Fallback (Offline)
            // =========================================================================
            if (!hasSufficientData(finalResult)) {
                try {
                    Log.d(TAG, "Attempting Layer 3: On-Device ML Kit Fallback...")
                    val mlKitResult = runMlKitFallback(frontBitmap, backBitmap)
                    mergeJsonData(finalResult, mlKitResult)
                } catch (e: Exception) {
                    Log.w(TAG, "Layer 3 (ML Kit) encountered an error: ${e.message}")
                }
            }

            // FIX: Removed "Layer 4" (filling missing CNIC / name / DOB / expiry from the logged-in
            // user's saved profile). When OCR failed, the scanner reported SUCCESS with the profile's
            // CNIC, so a blurry or different person's card was never caught, and the error dialogs
            // (quota, API key, server busy) could never appear. Values now come only from the card.

            // =========================================================================
            // Final Evaluation & Dispatch
            // =========================================================================
            withContext(Dispatchers.Main) {
                if (hasSufficientData(finalResult) && !isValidCnicResult(finalResult)) {
                    // Something was read, but it is not a CNIC (no 13-digit CNIC number): fill nothing
                    onError(NOT_A_CNIC_MESSAGE)
                } else if (hasSufficientData(finalResult)) {
                    Log.i(TAG, "CNIC extraction successful")
                    onResult(finalResult)
                } else {
                    Log.e(TAG, "CNIC extraction could not identify details from card images. Last AI error: $lastAiError")
                    // Include the last AI/HTTP error (e.g. "429", "503") so the scanner can show the right dialog
                    val detail = if (lastAiError.isNotEmpty()) " [$lastAiError]" else ""
                    onError("Unable to extract CNIC details automatically. Please ensure the card image is clear, well-lit, and not blurred, or enter details manually.$detail")
                }
            }
        }
    }

    /** Shown (and matched by the scanner) when the picture is not a Pakistani CNIC. */
    const val NOT_A_CNIC_MESSAGE = "This picture does not look like a Pakistani CNIC (not a cnic). Please scan the front and back of your original CNIC."

    /** A real CNIC result: not marked "not a CNIC" and has a 13-digit CNIC number. */
    private fun isValidCnicResult(json: JSONObject): Boolean {
        if (json.optString("isCnic", "").equals("false", ignoreCase = true)) return false
        return json.optString("cnicNumber", "").count { it.isDigit() } == 13
    }

    private fun hasSufficientData(json: JSONObject): Boolean {
        val cnic = json.optString("cnicNumber", "").trim()
        val name = json.optString("nameEn", "").trim()
        val dob = json.optString("dateOfBirth", "").trim()
        return cnic.isNotEmpty() || (name.isNotEmpty() && dob.isNotEmpty())
    }

    private fun mergeJsonData(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = source.optString(key, "").trim()
            if (value.isNotEmpty() && target.optString(key, "").trim().isEmpty()) {
                target.put(key, value)
            }
        }
    }

    /**
     * Directly calls Google Gemini Vision API using lightweight base64 payloads.
     */
    private fun callDirectGeminiVision(frontBitmap: Bitmap, backBitmap: Bitmap?): JSONObject? {
        val models = listOf("gemini-3.5-flash-lite", "gemini-flash-latest")
        val frontB64 = bitmapToBase64(frontBitmap, 1200)
        val backB64 = if (backBitmap != null) bitmapToBase64(backBitmap, 1200) else null

        val prompt = (
            "You are an expert Pakistani CNIC (National Identity Card) data extraction system.\n" +
            "Analyze the provided CNIC image(s) (Front and/or Back) and extract all details with 100% precision.\n" +
            "Guidelines:\n" +
            "0. isCnic: true ONLY if the images really show a Pakistani CNIC / NICOP / SNIC identity card. If it is anything else\n" +
            "   (another card, a document, a screen, a person, an object) set isCnic to false and leave every other field empty.\n" +
            "   Never guess or invent values that are not printed on the card.\n" +
            "1. One or both images may be oriented sideways, upside down (180 degrees), or rotated. Read the text accurately regardless of orientation.\n" +
            "2. cnicNumber: Exact 13 digits with dashes in format XXXXX-XXXXXXX-X (e.g. 37405-3290376-5).\n" +
            "3. nameEn: Full name in English exactly as printed on the card.\n" +
            "4. fatherNameEn: Father or Husband name in English.\n" +
            "5. dateOfBirth: Date of birth formatted as DD-MMM-YYYY (e.g. 01-Jul-2002).\n" +
            "6. issueDate: Date of issue formatted as DD-MMM-YYYY (e.g. 26-Sep-2020).\n" +
            "7. expiryDate: Date of expiry formatted as DD-MMM-YYYY (e.g. 26-Sep-2030).\n" +
            "8. gender: 'Male' or 'Female' (or 'M' / 'F').\n" +
            "9. presentAddress: Current/present address translated or transliterated into clear English.\n" +
            "10. permanentAddress: Permanent address translated or transliterated into clear English.\n" +
            "Return ONLY a valid JSON object with keys: isCnic, cnicNumber, nameEn, fatherNameEn, dateOfBirth, issueDate, expiryDate, gender, presentAddress, permanentAddress."
        )

        for (model in models) {
            try {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", prompt))

                // Front image
                val frontInline = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", frontB64)
                }
                partsArray.put(JSONObject().put("inlineData", frontInline))

                // Optional Back image
                if (backB64 != null) {
                    val backInline = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", backB64)
                    }
                    partsArray.put(JSONObject().put("inlineData", backInline))
                }

                val contentObj = JSONObject().put("parts", partsArray)
                val contentsArray = JSONArray().put(contentObj)

                val requestJson = JSONObject().apply {
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$GEMINI_API_KEY"
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestJson.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errStr = response.body?.string() ?: ""
                        Log.w(TAG, "Direct Gemini call failed with code ${response.code} for model $model: $errStr")
                        lastAiError = "HTTP ${response.code} ${errStr.take(120)}"
                        return@use
                    }

                    val respBody = response.body?.string() ?: ""
                    val rootJson = JSONObject(respBody)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val first = candidates.getJSONObject(0)
                        val content = first.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var text = parts.getJSONObject(0).optString("text", "").trim()
                            if (text.startsWith("```")) {
                                text = text.replace(Regex("^```(?:json)?|```$"), "").trim()
                            }
                            if (text.isNotEmpty()) {
                                val parsed = JSONObject(text)
                                return normalizeExtractedJson(parsed)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error invoking model $model: ${e.message}")
            }
        }
        return null
    }

    private fun normalizeExtractedJson(src: JSONObject): JSONObject {
        val dest = JSONObject()
        fun copy(targetKey: String, vararg sourceKeys: String) {
            for (k in sourceKeys) {
                val v = src.optString(k, "").trim()
                if (v.isNotEmpty()) {
                    dest.put(targetKey, v)
                    return
                }
            }
        }

        copy("isCnic", "isCnic", "is_cnic")
        copy("cnicNumber", "cnicNumber", "cnic", "cnic_number", "CNIC")
        copy("nameEn", "nameEn", "name", "full_name", "fullName", "NAME")
        copy("fatherNameEn", "fatherNameEn", "fatherName", "father_name", "husbandName")
        copy("dateOfBirth", "dateOfBirth", "dob", "birth_date")
        copy("issueDate", "issueDate", "issue_date")
        copy("expiryDate", "expiryDate", "expiry_date")
        copy("gender", "gender", "Gender")
        copy("presentAddress", "presentAddress", "present_address", "current_address", "address")
        copy("permanentAddress", "permanentAddress", "permanent_address", "address")
        return dest
    }

    /**
     * Removes the background (fingers, table) around the card in a frame-cropped CNIC photo, on the phone.
     * The card's border is a long straight line where brightness changes sharply, so for each side the
     * strongest such line in the outer 15% of the photo is taken as the card edge. Returns null when no
     * clear edge is found or the result does not have a CNIC card's shape, so the caller keeps the photo.
     */
    fun trimToCardEdges(bitmap: Bitmap): Bitmap? {
        return try {
            val step = 4
            val w = bitmap.width / step
            val h = bitmap.height / step
            if (w < 40 || h < 40) return null
            val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
            val px = IntArray(w * h)
            small.getPixels(px, 0, w, 0, 0, w, h)
            if (small != bitmap) small.recycle()
            val lum = FloatArray(w * h) { i ->
                val c = px[i]
                (((c shr 16) and 0xFF) * 0.299f + ((c shr 8) and 0xFF) * 0.587f + (c and 0xFF) * 0.114f)
            }
            // Column profile: horizontal brightness change, using the middle 60% of rows (avoids corners/fingers)
            val colP = FloatArray(w)
            for (x in 1 until w - 1) {
                var sum = 0f
                for (y in (h * 0.2).toInt() until (h * 0.8).toInt()) sum += Math.abs(lum[y * w + x + 1] - lum[y * w + x - 1])
                colP[x] = sum
            }
            val rowP = FloatArray(h)
            for (y in 1 until h - 1) {
                var sum = 0f
                for (x in (w * 0.2).toInt() until (w * 0.8).toInt()) sum += Math.abs(lum[(y + 1) * w + x] - lum[(y - 1) * w + x])
                rowP[y] = sum
            }
            fun median(p: FloatArray): Float = p.copyOf().also { it.sort() }[p.size / 2].coerceAtLeast(0.0001f)
            fun pick(p: FloatArray, n: Int, fromStart: Boolean): Int {
                val band = (n * 0.15).toInt()
                val range = if (fromStart) 1 until band else (n - band) until n - 1
                var best = -1; var bestVal = 0f
                for (i in range) if (p[i] > bestVal) { bestVal = p[i]; best = i }
                val strongEdge = best >= 0 && bestVal > 2.2f * median(p)
                return if (strongEdge) best else if (fromStart) 0 else n
            }
            val left = pick(colP, w, true) * step
            val right = pick(colP, w, false) * step
            val top = pick(rowP, h, true) * step
            val bottom = pick(rowP, h, false) * step
            val cw = (right - left).coerceAtMost(bitmap.width - left)
            val ch = (bottom - top).coerceAtMost(bitmap.height - top)
            if (cw <= 0 || ch <= 0) return null
            if (cw >= bitmap.width - step && ch >= bitmap.height - step) return null // nothing to remove
            // keep it only if it still looks like the whole card (85.6 x 54 mm = 1.585)
            val ratio = maxOf(cw, ch).toDouble() / minOf(cw, ch)
            if (ratio < 1.4 || ratio > 1.8) return null
            if (cw.toLong() * ch < bitmap.width.toLong() * bitmap.height * 0.55) return null
            Bitmap.createBitmap(bitmap, left, top, cw, ch)
        } catch (e: Exception) {
            Log.w(TAG, "Card edge trim skipped: ${e.message}")
            null
        }
    }

    /**
     * Local Google ML Kit Text Recognition fallback.
     */
    private suspend fun runMlKitFallback(frontBitmap: Bitmap, backBitmap: Bitmap?): JSONObject {
        val result = JSONObject()
        val frontLines = recognizeTextMlKit(frontBitmap)
        
        // If front didn't yield CNIC, try 180° rotation in case card was upside down
        var detectedCnic = extractCnicNumber(frontLines)
        var usedLines = frontLines
        if (detectedCnic.isEmpty()) {
            val rotated180 = rotateBitmap(frontBitmap, 180f)
            val rotatedLines = recognizeTextMlKit(rotated180)
            val cnicRotated = extractCnicNumber(rotatedLines)
            if (cnicRotated.isNotEmpty()) {
                detectedCnic = cnicRotated
                usedLines = rotatedLines
            }
        }

        if (detectedCnic.isNotEmpty()) {
            result.put("cnicNumber", detectedCnic)
        }

        // Dates: DOB, Issue, Expiry
        val detectedDates = extractDates(usedLines)
        if (detectedDates.isNotEmpty()) {
            val sorted = detectedDates.sortedBy { it.second }
            if (sorted.size >= 1) result.put("dateOfBirth", sorted[0].first)
            if (sorted.size == 2) {
                result.put("expiryDate", sorted[1].first)
            } else if (sorted.size >= 3) {
                result.put("issueDate", sorted[1].first)
                result.put("expiryDate", sorted[2].first)
            }
        }

        // Gender
        val gender = extractGender(usedLines)
        if (gender.isNotEmpty()) {
            result.put("gender", gender)
        }

        // Name heuristic from front
        val name = extractName(usedLines)
        if (name.isNotEmpty()) {
            result.put("nameEn", name)
        }

        // Back bitmap text for address if available
        if (backBitmap != null) {
            val backLines = recognizeTextMlKit(backBitmap)
            val address = backLines.filter { it.length > 10 }.joinToString(", ")
            if (address.isNotEmpty()) {
                result.put("presentAddress", address)
            }
        }

        return result
    }

    private suspend fun recognizeTextMlKit(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { it.text.trim() }
                }.filter { it.isNotEmpty() }
                cont.resume(lines)
            }
            .addOnFailureListener {
                cont.resume(emptyList())
            }
    }

    private fun extractCnicNumber(lines: List<String>): String {
        val cnicRegex = Regex("\\b\\d{5}[-\\s.]?\\d{7}[-\\s.]?\\d{1}\\b")
        for (line in lines) {
            val m = cnicRegex.find(line)
            if (m != null) {
                val digits = m.value.filter { it.isDigit() }
                if (digits.length == 13) {
                    return "${digits.substring(0, 5)}-${digits.substring(5, 12)}-${digits.substring(12)}"
                }
            }
        }
        return ""
    }

    private fun extractDates(lines: List<String>): List<Pair<String, Long>> {
        val dateRegex = Regex("\\b\\d{2}[\\.\\-/]\\d{2}[\\.\\-/]\\d{4}\\b")
        val results = mutableListOf<Pair<String, Long>>()
        for (line in lines) {
            val matches = dateRegex.findAll(line)
            for (m in matches) {
                val raw = m.value
                val cleaned = raw.replace(".", "-").replace("/", "-")
                val parts = cleaned.split("-")
                if (parts.size == 3) {
                    try {
                        val d = parts[0].toInt()
                        val mo = parts[1].toInt() - 1
                        val y = parts[2].toInt()
                        val cal = Calendar.getInstance().apply { set(y, mo, d) }
                        results.add(Pair(raw, cal.timeInMillis))
                    } catch (ignored: Exception) {}
                }
            }
        }
        return results.distinctBy { it.first }
    }

    private fun extractGender(lines: List<String>): String {
        for (line in lines) {
            val upper = line.uppercase()
            if (upper.contains("GENDER")) {
                if (upper.contains("M") && !upper.contains("FEMALE")) return "Male"
                if (upper.contains("F") || upper.contains("FEMALE")) return "Female"
            }
        }
        return ""
    }

    private fun extractName(lines: List<String>): String {
        val nameIndex = lines.indexOfFirst { it.equals("Name", ignoreCase = true) }
        if (nameIndex != -1 && nameIndex + 1 < lines.size) {
            val candidate = lines[nameIndex + 1].trim()
            if (candidate.matches(Regex("^[a-zA-Z\\s]{3,35}$"))) {
                return candidate
            }
        }
        return ""
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int): String {
        var scaled = bitmap
        val width = bitmap.width
        val height = bitmap.height
        val maxActual = maxOf(width, height)
        if (maxActual > maxDimension) {
            val scale = maxDimension.toFloat() / maxActual
            scaled = Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        }
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun bitmapToMultipart(bitmap: Bitmap, partName: String, filename: String): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = byteArray.toRequestBody(mediaType)
        return MultipartBody.Part.createFormData(partName, filename, requestBody)
    }

    private fun copyGsonField(dest: JSONObject, src: com.google.gson.JsonObject, targetKey: String, vararg sourceKeys: String) {
        for (k in sourceKeys) {
            if (src.has(k) && !src.get(k).isJsonNull) {
                val v = src.get(k).asString.trim()
                if (v.isNotEmpty()) {
                    dest.put(targetKey, v)
                    return
                }
            }
        }
    }

    private fun copyGsonFieldIfEmpty(dest: JSONObject, src: com.google.gson.JsonObject, targetKey: String, vararg sourceKeys: String) {
        if (dest.optString(targetKey).isEmpty()) {
            copyGsonField(dest, src, targetKey, *sourceKeys)
        }
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            CnicRotationHelper.loadBitmapWithExif(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from Uri: $uri", e)
            null
        }
    }
}
