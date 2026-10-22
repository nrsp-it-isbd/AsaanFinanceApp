package com.assan.finance.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Speaks an assistant reply out loud.
 * First choice: natural Gemini voice ([AsaanAiEngine.synthesizeSpeech]).
 * Fallback: the phone's own TextToSpeech (Urdu if available, otherwise English).
 *
 * Usable from Kotlin and Java. Call [release] when the screen/dialog closes.
 */
class VoiceReplyPlayer(context: Context) {

    interface Listener {
        /** Audio actually started playing (after the voice was generated). */
        fun onSpeakingStarted()
        /** Finished speaking (or could not speak). Not called after [stop]. */
        fun onSpeakingDone()
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val main = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var token = 0 // invalidates callbacks from a previous speak() after stop()

    init {
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.setSpeechRate(0.95f)
        }
    }

    fun speak(text: String, listener: Listener) {
        stop()
        val myToken = ++token
        job = scope.launch {
            val file = AsaanAiEngine.synthesizeSpeech(appContext, text)
            if (!isActive || myToken != token) return@launch
            if (file != null && playFile(file, myToken, listener)) return@launch
            speakWithPhoneTts(text, myToken, listener)
        }
    }

    val isSpeaking: Boolean
        get() = (mediaPlayer?.isPlaying == true) || (tts?.isSpeaking == true) || (job?.isActive == true)

    /** Stops generation and playback immediately. The listener is not called. */
    fun stop() {
        token++
        job?.cancel()
        job = null
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stop()
        scope.cancel()
        tts?.shutdown()
        tts = null
    }

    private fun playFile(file: File, myToken: Int, listener: Listener): Boolean {
        return try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.setOnCompletionListener {
                file.delete()
                if (myToken == token) {
                    mediaPlayer?.release()
                    mediaPlayer = null
                    listener.onSpeakingDone()
                }
            }
            mediaPlayer = player
            player.start()
            listener.onSpeakingStarted()
            true
        } catch (e: Exception) {
            Log.w("VoiceReplyPlayer", "Could not play generated voice: ${e.message}")
            file.delete()
            false
        }
    }

    private fun speakWithPhoneTts(text: String, myToken: Int, listener: Listener) {
        val engine = tts
        val spoken = AsaanAiEngine.cleanForSpeech(text)
        if (engine == null || !ttsReady || spoken.isBlank()) {
            listener.onSpeakingDone()
            return
        }
        val english = AsaanAiEngine.looksEnglish(spoken)
        val primary = if (english) Locale.US else Locale("ur", "PK")
        val result = engine.setLanguage(primary)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale("en", "IN")) // reads Roman Urdu better than en-US
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { if (myToken == token) listener.onSpeakingStarted() }
            }
            override fun onDone(utteranceId: String?) {
                main.post { if (myToken == token) listener.onSpeakingDone() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { if (myToken == token) listener.onSpeakingDone() }
            }
        })
        val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "assistant_reply") }
        engine.speak(spoken, TextToSpeech.QUEUE_FLUSH, params, "assistant_reply")
    }
}
