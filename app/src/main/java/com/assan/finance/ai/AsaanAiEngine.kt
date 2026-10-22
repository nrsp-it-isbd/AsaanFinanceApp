package com.assan.finance.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Shared AI engine for both assistants (Home "Asaan Assistant" and the Financing Form assistant).
 *
 *  - [generate]: a "thinking" Gemini model reasons about the request first, then answers.
 *    Several models are tried in order, because any single model can be overloaded (503) or
 *    out of quota (429) at a given moment.
 *  - [synthesizeSpeech]: natural Urdu / English voice from a Gemini TTS model (returns a WAV file).
 *
 * NOTE: the API key is shipped inside the app (as it was before). Anyone with the APK can
 * extract it. Move these calls behind the backend (Ai/voice-chat) before a wide release.
 */
object AsaanAiEngine {

    private const val TAG = "AsaanAiEngine"
    // Key comes from local.properties (GEMINI_API_KEY) or the CI secret, never from source control
    private val API_KEY = com.assan.finance.BuildConfig.GEMINI_API_KEY
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    /** Best first. All support thinking; on 429/503/404 the next one is tried. */
    private val CHAT_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )

    /** Verified to speak natural Urdu from both Roman Urdu and Urdu script. */
    private val TTS_MODELS = listOf(
        "gemini-3.8-flash-tts",
        "gemini-2.5-flash-preview-tts",
        "gemini-3.1-flash-tts-preview"
    )

    /** Calm, clear male voice; suits a formal financial assistant. */
    private const val VOICE_NAME = "Charon"

    data class Turn(val fromUser: Boolean, val text: String)

    private val chatClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val ttsClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaTypeOrNull()

    // ───────────────────────────── Chat ─────────────────────────────

    /**
     * Sends the conversation to a thinking model and returns the answer text (or JSON text when
     * [json] is true). Returns null only if every model failed.
     *
     * @param thinkingBudget tokens the model may spend reasoning before answering
     *                       (lower = faster; voice uses a smaller budget than text).
     */
    suspend fun generate(
        systemPrompt: String,
        history: List<Turn>,
        userText: String,
        json: Boolean = false,
        thinkingBudget: Int = 1024
    ): String? = withContext(Dispatchers.IO) {
        val contents = buildContents(history, userText)
        for (model in CHAT_MODELS) {
            // Each model gets one retry when it is only briefly overloaded
            for (attempt in 0..1) {
                val result = callChat(model, systemPrompt, contents, json, thinkingBudget)
                when (result.status) {
                    CallStatus.OK -> return@withContext result.text
                    CallStatus.BAD_THINKING_CONFIG -> {
                        val retry = callChat(model, systemPrompt, contents, json, null)
                        if (retry.status == CallStatus.OK) return@withContext retry.text
                        break
                    }
                    CallStatus.OVERLOADED -> if (attempt == 0) kotlinx.coroutines.delay(600) else break
                    CallStatus.SKIP_MODEL -> break
                }
            }
        }
        null
    }

    private enum class CallStatus { OK, OVERLOADED, SKIP_MODEL, BAD_THINKING_CONFIG }
    private data class CallResult(val status: CallStatus, val text: String? = null)

    private fun callChat(
        model: String,
        systemPrompt: String,
        contents: JSONArray,
        json: Boolean,
        thinkingBudget: Int?
    ): CallResult {
        val generationConfig = JSONObject().apply {
            put("temperature", 0.6)
            if (json) put("responseMimeType", "application/json")
            if (thinkingBudget != null) {
                put("thinkingConfig", JSONObject().put("thinkingBudget", thinkingBudget))
            }
        }
        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", contents)
            put("generationConfig", generationConfig)
        }
        val request = Request.Builder()
            .url("$BASE_URL$model:generateContent?key=$API_KEY")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        return try {
            chatClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Chat $model -> ${resp.code}: ${raw.take(200)}")
                    return when {
                        resp.code == 400 && raw.contains("thinking", ignoreCase = true) -> CallResult(CallStatus.BAD_THINKING_CONFIG)
                        resp.code == 503 || resp.code == 500 -> CallResult(CallStatus.OVERLOADED)
                        else -> CallResult(CallStatus.SKIP_MODEL) // 429 quota, 404 retired model, ...
                    }
                }
                val text = extractText(raw)
                if (text.isNullOrBlank()) CallResult(CallStatus.SKIP_MODEL)
                else {
                    Log.d(TAG, "Chat answered by $model")
                    CallResult(CallStatus.OK, text)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Chat $model failed: ${e.message}")
            CallResult(CallStatus.SKIP_MODEL)
        }
    }

    /** Joins the answer parts, skipping the model's internal "thought" parts. */
    private fun extractText(raw: String): String? {
        val candidates = JSONObject(raw).optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: return null
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (p.optBoolean("thought", false)) continue
            sb.append(p.optString("text", ""))
        }
        return sb.toString().trim()
    }

    /** Gemini requires alternating user/model turns that start with a user turn. */
    private fun buildContents(history: List<Turn>, userText: String): JSONArray {
        val turns = mutableListOf<Pair<String, StringBuilder>>()
        for (t in history.takeLast(16) + Turn(true, userText)) {
            val role = if (t.fromUser) "user" else "model"
            val text = t.text.trim()
            if (text.isEmpty()) continue
            if (turns.isNotEmpty() && turns.last().first == role) turns.last().second.append("\n").append(text)
            else turns.add(role to StringBuilder(text))
        }
        if (turns.isNotEmpty() && turns.first().first == "model") turns.add(0, "user" to StringBuilder("Assalam-o-Alaikum"))
        val arr = JSONArray()
        for ((role, sb) in turns) {
            arr.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", sb.toString()))))
        }
        return arr
    }

    // ───────────────────────────── Voice ─────────────────────────────

    /**
     * Turns [text] into natural speech and writes it to a WAV file in the cache.
     * Returns null if every TTS model failed (the caller then uses Android TTS).
     */
    suspend fun synthesizeSpeech(context: Context, text: String): File? = withContext(Dispatchers.IO) {
        val spoken = cleanForSpeech(text)
        if (spoken.isBlank()) return@withContext null
        val style = if (looksEnglish(spoken))
            "Read this aloud in a warm, polite and professional voice with a clear Pakistani English accent:"
        else
            "Read this aloud in natural, formal and respectful Pakistani Urdu, warm and calm, like a helpful bank officer:"
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "$style\n$spoken")))))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().put("voiceConfig",
                    JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", VOICE_NAME))))
            })
        }.toString()

        for (model in TTS_MODELS) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL$model:generateContent?key=$API_KEY")
                    .post(body.toRequestBody(JSON_MEDIA))
                    .build()
                ttsClient.newCall(request).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "TTS $model -> ${resp.code}: ${raw.take(150)}")
                        return@use
                    }
                    val inline = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                        ?.optJSONObject("inlineData") ?: return@use
                    val bytes = Base64.decode(inline.optString("data"), Base64.DEFAULT)
                    val mime = inline.optString("mimeType")
                    val wav = if (mime.contains("wav", ignoreCase = true)) bytes else pcmToWav(bytes, sampleRate(mime))
                    val file = File(context.cacheDir, "assistant_voice_${System.currentTimeMillis()}.wav")
                    file.writeBytes(wav)
                    Log.d(TAG, "TTS by $model, ${wav.size} bytes")
                    return@withContext file
                }
            } catch (e: Exception) {
                Log.w(TAG, "TTS $model failed: ${e.message}")
            }
        }
        null
    }

    /**
     * Cleans speech-recognition text before it goes to the model:
     * Urdu / Arabic digits (۰-۹, ٠-٩) -> 0-9, and digit groups split by spaces or dashes
     * ("35202 1234567 1", "35202-1234567-1") are joined so a dictated CNIC / phone stays one number.
     */
    fun normalizeSpokenText(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
        var out = sb.toString()
        // join runs like "35202 1234567 1" / "0300 1234567" into one number
        out = Regex("(?<!\\d)\\d[\\d\\s\\-]{8,}\\d(?!\\d)").replace(out) { m ->
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 10..16) digits else m.value
        }
        return out.replace(Regex("\\s+"), " ").trim()
    }

    /** Safety net for the wording rules: Hindi words -> Pakistani Urdu, no markdown bold. */
    fun polishReply(text: String): String =
        text.replace(Regex("(?i)\\bkripya\\b"), "baraye meherbani")
            .replace(Regex("(?i)\\bdhanyawad\\b"), "shukriya")
            .replace(Regex("(?i)\\bsuvidha\\b"), "sahulat")
            .replace(Regex("(?i)\\bjankari\\b"), "maloomat")
            .replace(Regex("(?i)\\bprapt\\b"), "hasil")
            .replace("**", "")
            .trim()

    /** Removes markdown, bullets and emojis so the voice reads clean sentences. */
    fun cleanForSpeech(text: String): String =
        text.replace(Regex("[*_#`>]"), " ")
            .replace(Regex("(?m)^\\s*(\\d+[.)]|[-•])\\s*"), "")
            .replace(Regex("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** True when the text is English rather than Roman Urdu / Urdu. */
    fun looksEnglish(text: String): Boolean {
        if (Regex("[\\u0600-\\u06FF]").containsMatchIn(text)) return false
        val words = text.lowercase(Locale.ROOT).split(Regex("[^a-z]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        val urduMarkers = setOf(
            "hai", "hain", "hoon", "hun", "ho", "ka", "ki", "ke", "ko", "se", "sy", "mein", "main", "mujhe", "muje",
            "aap", "ap", "apni", "apna", "kya", "kia", "kaise", "kese", "nahi", "nahin", "chahiye", "chahye",
            "karein", "kare", "karna", "batayein", "bataen", "ji", "jee", "shukriya", "meherbani", "raqam",
            "baraye", "zaroor", "theek", "liye", "kitna", "kitni", "aur", "ya", "par", "pr", "tou", "to"
        )
        val urduCount = words.count { it in urduMarkers }
        return urduCount * 4 < words.size
    }

    private fun sampleRate(mime: String): Int =
        Regex("rate=(\\d+)").find(mime)?.groupValues?.get(1)?.toIntOrNull() ?: 24000

    private fun pcmToWav(pcm: ByteArray, sampleRate: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val byteRate = sampleRate * 2
        fun intLE(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte())
        fun shortLE(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
        out.write("RIFF".toByteArray()); out.write(intLE(36 + pcm.size)); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); out.write(intLE(16)); out.write(shortLE(1)); out.write(shortLE(1))
        out.write(intLE(sampleRate)); out.write(intLE(byteRate)); out.write(shortLE(2)); out.write(shortLE(16))
        out.write("data".toByteArray()); out.write(intLE(pcm.size)); out.write(pcm)
        return out.toByteArray()
    }
}
