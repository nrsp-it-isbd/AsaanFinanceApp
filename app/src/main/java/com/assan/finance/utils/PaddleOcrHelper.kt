package com.assan.finance.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class for Offline CNIC Extraction using PaddleOCR (via JNI)
 * with a robust fallback to ML Kit when native libraries are not present.
 */
class PaddleOcrHelper(private val context: Context) {

    companion object {
        private const val TAG = "PaddleOcrHelper"
        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("paddle_ocr_jni")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library 'paddle_ocr_jni' not found. Falling back to ML Kit for offline mode.", e)
            }
        }
    }

    // Native JNI interface declaration for PaddleOCR
    private external fun nativeInit(context: Context, detModel: String, recModel: String, clsModel: String): Boolean
    private external fun nativeDetect(bitmap: Bitmap): Array<String>
    private external fun nativeRelease()

    fun initEngine(detModel: String = "models/det.nb", recModel: String = "models/rec.nb", clsModel: String = "models/cls.nb"): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            nativeInit(context, detModel, recModel, clsModel)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize native PaddleOCR engine", e)
            false
        }
    }

    fun releaseEngine() {
        if (isLibraryLoaded) {
            try {
                nativeRelease()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release native PaddleOCR engine", e)
            }
        }
    }

    /**
     * Step 1: Preprocesses the CNIC bitmap (Grayscale + High Contrast)
     * to make text lines sharper and easier for OCR engines to recognize.
     */
    fun preprocessBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()

        // Grayscale conversion + High Contrast adjustment
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Turn to grayscale
            val contrast = 1.8f
            val translate = -128f * contrast + 128f
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    /**
     * Runs text detection on a single bitmap.
     * Uses PaddleOCR JNI if loaded, otherwise falls back to ML Kit.
     * Checks both raw and preprocessed bitmaps for maximum accuracy.
     */
    suspend fun detectText(bitmap: Bitmap): List<String> {
        val detected = mutableListOf<String>()

        // 1. First run on raw bitmap (ML Kit handles color images exceptionally well)
        try {
            detected.addAll(runMlKitOcr(bitmap))
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit raw bitmap failed", e)
        }

        // 2. Also run on preprocessed contrast bitmap if JNI is loaded or to catch faint characters
        try {
            val preprocessed = preprocessBitmap(bitmap)
            if (isLibraryLoaded) {
                try {
                    val results = nativeDetect(preprocessed)
                    detected.addAll(results)
                } catch (e: Exception) {
                    Log.e(TAG, "PaddleOCR native detection failed", e)
                }
            } else {
                detected.addAll(runMlKitOcr(preprocessed))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Preprocessed detection failed", e)
        }

        return detected.distinct()
    }

    private suspend fun runMlKitOcr(bitmap: Bitmap): List<String> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { it.text }
                }
                continuation.resume(lines)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    private fun cleanCnicDigits(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            if (char.isDigit()) {
                sb.append(char)
            } else {
                when (char) {
                    'I', 'l', 'i', '|', '/' -> sb.append('1')
                    'O', 'o', 'Q' -> sb.append('0')
                    'S', 's' -> sb.append('5')
                    'Z', 'z' -> sb.append('2')
                    'B' -> sb.append('8')
                    'g' -> sb.append('9')
                    'A', 'a' -> sb.append('4')
                }
            }
        }
        return sb.toString()
    }

    /**
     * Extracts fields from the raw OCR texts of both sides of CNIC.
     */
    fun parseCnicData(frontTexts: List<String>, backTexts: List<String>): CnicData {
        val cnicData = CnicData()
        val allTexts = frontTexts + backTexts
        val cnicRegex = Regex("\\d{5}[\\s\\-.]?\\d{7}[\\s\\-.]?\\d")
        val dateRegex = Regex("\\d{2}[\\.\\-/]\\d{2}[\\.\\-/]\\d{4}")

        // 1. Extract CNIC Number: Direct search for 13-digit sequence across all words/lines
        for (line in allTexts) {
            val stripped = line.replace(" ", "").replace("-", "").replace(".", "")
            val cleanedDigits = cleanCnicDigits(stripped)
            val match = Regex("\\d{13}").find(cleanedDigits)
            if (match != null) {
                val d = match.value
                cnicData.cnicNumber = "${d.substring(0, 5)}-${d.substring(5, 12)}-${d.substring(12)}"
                break
            }
        }

        // Fallback candidates
        if (cnicData.cnicNumber.isEmpty()) {
            val candidates = allTexts.flatMap { line ->
                line.split(Regex("[^a-zA-Z0-9|/\\-.]"))
            }.map { chunk ->
                chunk.trim()
            }.filter { chunk ->
                val digits = cleanCnicDigits(chunk)
                digits.length == 13
            }

            if (candidates.isNotEmpty()) {
                val cleanDigits = cleanCnicDigits(candidates[0])
                cnicData.cnicNumber = "${cleanDigits.substring(0, 5)}-${cleanDigits.substring(5, 12)}-${cleanDigits.substring(12)}"
            } else {
                for (line in allTexts) {
                    val match = cnicRegex.find(line)
                    if (match != null) {
                        val cleanDigits = match.value.filter { it.isDigit() }
                        if (cleanDigits.length == 13) {
                            cnicData.cnicNumber = "${cleanDigits.substring(0, 5)}-${cleanDigits.substring(5, 12)}-${cleanDigits.substring(12)}"
                            break
                        }
                    }
                }
            }
        }

        // 2. Extract Dates (DOB, Issue Date, Expiry Date)
        val detectedDates = mutableListOf<String>()
        for (line in allTexts) {
            val matches = dateRegex.findAll(line)
            for (m in matches) {
                detectedDates.add(m.value)
            }
        }

        // Clean duplicates
        val uniqueDates = detectedDates.distinct()
        
        // Smart parse logic for dates based on labeling or sorting heuristics
        if (uniqueDates.isNotEmpty()) {
            val parsedDates = uniqueDates.mapNotNull { dateStr ->
                try {
                    val cleaned = dateStr.replace(".", "-").replace("/", "-")
                    val parts = cleaned.split("-")
                    if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt() - 1
                        val year = parts[2].toInt()
                        val cal = Calendar.getInstance().apply { set(year, month, day) }
                        Pair(dateStr, cal.time)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.second }

            if (parsedDates.size >= 1) {
                val dobRaw = parsedDates[0].first
                cnicData.dateOfBirth = CommonFunctions.formatDateOfBirth(dobRaw).ifEmpty { dobRaw }
            }
            if (parsedDates.size >= 2) {
                cnicData.issueDate = parsedDates[1].first
            }
            if (parsedDates.size >= 3) {
                cnicData.expiryDate = parsedDates[2].first
            }
        }

        // 3. Extract Name & Father's Name from Front Side
        // Heuristic: search for ALL CAPS names first
        val capsLines = frontTexts.map { it.trim() }.filter { line ->
            line.matches(Regex("^[A-Z\\s]{3,30}$")) &&
            !line.contains("PAKISTAN", ignoreCase = true) &&
            !line.contains("REPUBLIC", ignoreCase = true) &&
            !line.contains("ISLAMIC", ignoreCase = true) &&
            !line.contains("IDENTITY", ignoreCase = true) &&
            !line.contains("CARD", ignoreCase = true) &&
            !line.contains("NAME", ignoreCase = true) &&
            !line.contains("FATHER", ignoreCase = true) &&
            !line.contains("HUSBAND", ignoreCase = true) &&
            !line.contains("GENDER", ignoreCase = true) &&
            !line.contains("COUNTRY", ignoreCase = true) &&
            !line.contains("SIGNATURE", ignoreCase = true) &&
            !line.contains("HOLDER", ignoreCase = true) &&
            !line.contains("AUTHORITY", ignoreCase = true)
        }

        if (capsLines.isNotEmpty()) {
            cnicData.nameEn = capsLines[0]
        }
        if (capsLines.size >= 2) {
            cnicData.fatherNameEn = capsLines[1]
        }

        // Fallback: mixed-case or relaxed match
        if (cnicData.nameEn.isEmpty()) {
            val filteredFront = frontTexts.map { it.trim() }.filter { line ->
                line.length > 3 &&
                line.any { it.isLetter() } &&
                !line.contains("Government", ignoreCase = true) &&
                !line.contains("Pakistan", ignoreCase = true) &&
                !line.contains("Identity", ignoreCase = true) &&
                !line.contains("Card", ignoreCase = true) &&
                !line.contains("Name", ignoreCase = true) &&
                !line.contains("Father", ignoreCase = true) &&
                !line.contains("Husband", ignoreCase = true) &&
                !line.contains("Date", ignoreCase = true) &&
                !line.contains("Birth", ignoreCase = true) &&
                !line.contains("Issue", ignoreCase = true) &&
                !line.contains("Expiry", ignoreCase = true) &&
                !line.contains("Gender", ignoreCase = true) &&
                !line.contains("Country", ignoreCase = true) &&
                !line.contains("Signature", ignoreCase = true) &&
                !line.contains("Holder", ignoreCase = true) &&
                !line.contains("Authority", ignoreCase = true)
            }

            if (filteredFront.isNotEmpty()) {
                cnicData.nameEn = filteredFront[0]
            }
            if (filteredFront.size >= 2) {
                cnicData.fatherNameEn = filteredFront[1]
            }
        }

        // 4. Address parsing from Back texts (supports English and Urdu semantic matching)
        var presentAddress = ""
        var permanentAddress = ""

        var isCollectingPresent = false
        var isCollectingPermanent = false

        val presentKeywords = listOf("موجودہ", "present", "pres")
        val permanentKeywords = listOf("مستقل", "permanent", "perm")
        val stopKeywords = listOf("رجسٹرار", "registrar", "general", "دستخط", "signature", "تاریخ", "date")

        for (line in backTexts) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Check if we hit present address label
            if (presentKeywords.any { trimmed.contains(it, ignoreCase = true) }) {
                isCollectingPresent = true
                isCollectingPermanent = false
                val parts = trimmed.split(Regex("[:۔]"))
                if (parts.size > 1) {
                    val content = parts.subList(1, parts.size).joinToString(" ").trim()
                    if (content.length > 3) {
                        presentAddress = content
                    }
                }
                continue
            }

            // Check if we hit permanent address label
            if (permanentKeywords.any { trimmed.contains(it, ignoreCase = true) }) {
                isCollectingPresent = false
                isCollectingPermanent = true
                val parts = trimmed.split(Regex("[:۔]"))
                if (parts.size > 1) {
                    val content = parts.subList(1, parts.size).joinToString(" ").trim()
                    if (content.length > 3) {
                        permanentAddress = content
                    }
                }
                continue
            }

            // Check if we hit a stop keyword (e.g. registrar details)
            if (stopKeywords.any { trimmed.contains(it, ignoreCase = true) }) {
                isCollectingPresent = false
                isCollectingPermanent = false
                continue
            }

            // Collect lines
            if (isCollectingPresent) {
                if (presentAddress.isEmpty()) {
                    presentAddress = trimmed
                } else {
                    presentAddress += ", $trimmed"
                }
            } else if (isCollectingPermanent) {
                if (permanentAddress.isEmpty()) {
                    permanentAddress = trimmed
                } else {
                    permanentAddress += ", $trimmed"
                }
            }
        }

        // Clean up potential leftover prefix strings
        val cleanLabelRegex = Regex("^(پتہ|پتہ:|address|address:)\\s*", RegexOption.IGNORE_CASE)
        presentAddress = presentAddress.replace(cleanLabelRegex, "").trim()
        permanentAddress = permanentAddress.replace(cleanLabelRegex, "").trim()

        // Structural Fallback if semantic extraction found nothing
        if (presentAddress.isEmpty() && permanentAddress.isEmpty()) {
            val addressLines = backTexts.map { it.trim() }.filter { line ->
                line.length > 5 &&
                !cnicRegex.containsMatchIn(line) &&
                !dateRegex.containsMatchIn(line) &&
                !line.contains("Registrar", ignoreCase = true) &&
                !line.contains("General", ignoreCase = true) &&
                !line.contains("رجسٹرار", ignoreCase = true)
            }
            if (addressLines.isNotEmpty()) {
                presentAddress = addressLines[0]
                if (addressLines.size >= 2) {
                    permanentAddress = addressLines.subList(1, addressLines.size).joinToString(", ")
                } else {
                    permanentAddress = addressLines[0]
                }
            }
        }

        cnicData.presentAddress = presentAddress
        cnicData.permanentAddress = permanentAddress

        // Default static values if empty
        if (cnicData.country.isEmpty()) cnicData.country = "Pakistan"
        
        return cnicData
    }
}
