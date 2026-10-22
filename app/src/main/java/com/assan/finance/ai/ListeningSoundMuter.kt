package com.assan.finance.ai

import android.content.Context
import android.media.AudioManager

/**
 * The Android speech recognizer plays a short start/stop tone every time it (re)starts. While the
 * assistant listens through pauses it restarts often, so the tone repeated on and off.
 * This mutes the tone streams for the whole listening phase and restores them before the assistant
 * thinks/speaks or when voice mode ends. Calls are idempotent; always call [restore] on exit.
 */
class ListeningSoundMuter(context: Context) {

    private val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // Media stream only. NEVER mute RING / NOTIFICATION / SYSTEM: on Android that switches the phone's
    // ringer to silent (the user would miss calls), and it can stay that way if the app is killed.
    private val streams = intArrayOf(AudioManager.STREAM_MUSIC)
    private var muted = false

    fun mute() {
        if (muted) return
        for (s in streams) {
            try { audio.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
        }
        muted = true
    }

    fun restore() {
        if (!muted) return
        for (s in streams) {
            try { audio.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
        }
        muted = false
    }
}
