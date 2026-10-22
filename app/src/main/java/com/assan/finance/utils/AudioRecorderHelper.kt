package com.assan.finance.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

import kotlinx.coroutines.*

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    var onAmplitudeChange: ((Float) -> Unit)? = null
    var onSilenceDetected: (() -> Unit)? = null

    // VAD Configuration
    private val silenceThreshold = 800 // Max amplitude threshold to consider as silence
    private val silenceDurationMs = 1500L // 1.5 seconds of silence
    private val pollIntervalMs = 50L // 50ms polling

    fun startRecording(): File? {
        stopRecording()
        
        val file = File(context.cacheDir, "voice_chat_${System.currentTimeMillis()}.m4a")
        outputFile = file
        
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        return try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            startAmplitudePolling()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                newRecorder.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            recorder = null
            outputFile = null
            null
        }
    }
    
    private fun startAmplitudePolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            var silenceTime = 0L
            var totalQuietTime = 0L
            var hasStartedSpeaking = false
            
            while (isActive && recorder != null) {
                try {
                    val maxAmplitude = recorder?.maxAmplitude ?: 0
                    onAmplitudeChange?.invoke(maxAmplitude.toFloat())
                    
                    if (maxAmplitude > silenceThreshold) {
                        hasStartedSpeaking = true
                        silenceTime = 0L // reset silence timer
                        totalQuietTime = 0L
                    } else {
                        if (hasStartedSpeaking) {
                            silenceTime += pollIntervalMs
                            if (silenceTime >= silenceDurationMs) {
                                // User stopped speaking
                                withContext(Dispatchers.Main) {
                                    onSilenceDetected?.invoke()
                                }
                                break
                            }
                        } else {
                            totalQuietTime += pollIntervalMs
                            if (totalQuietTime >= 5000L) {
                                // 5 seconds of complete silence from start, force stop to prevent infinite listening
                                withContext(Dispatchers.Main) {
                                    onSilenceDetected?.invoke()
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun stopRecording() {
        pollJob?.cancel()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
        }
    }
    
    fun release() {
        stopRecording()
        scope.cancel()
    }
}
