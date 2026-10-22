package com.assan.finance.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.assan.finance.R
import com.assan.finance.databinding.FragmentVoiceChatBinding
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class VoiceChatFragment : DialogFragment() {

    private var _binding: FragmentVoiceChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VoiceChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    // ── Voice Mode Engine ──
    // Voice-to-voice loop like ChatGPT voice: listen -> think -> speak -> listen again.
    private var speechRecognizer: SpeechRecognizer? = null
    private var voicePlayer: com.assan.finance.ai.VoiceReplyPlayer? = null
    private var isVoiceModeActive = false  // true while voice overlay is visible

    // Manual send tracking variables
    private val silenceHandler = Handler(Looper.getMainLooper())
    private val SILENCE_TIMEOUT_MS = 1500L
    private var userHasSpoken = false
    private var lastPartialResult = ""
    private var accumulatedText = ""
    private var isWaitingForSend = false
    private var beepMuter: com.assan.finance.ai.ListeningSoundMuter? = null
    private var consecutiveSpeechErrors = 0

    // The user paused after speaking: send automatically (no need to tap)
    private val silenceRunnable = Runnable {
        if (isVoiceModeActive && isWaitingForSend && (accumulatedText.isNotBlank() || lastPartialResult.isNotBlank())) {
            onUserTappedSend()
        }
    }

    // Network listener for auto-retry
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), R.string.auto_microphone_permission_is_655, Toast.LENGTH_LONG).show()
            dismiss()
        }
    }

    // ───────────────────── Lifecycle ─────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVoiceChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        voicePlayer = com.assan.finance.ai.VoiceReplyPlayer(requireContext())
        beepMuter = com.assan.finance.ai.ListeningSoundMuter(requireContext())
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        setupNetworkListener()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels * 0.92).toInt()
            val height = (metrics.heightPixels * 0.50f).toInt()
            setLayout(width, height)
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.5f)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        silenceHandler.removeCallbacksAndMessages(null)
        destroySpeechRecognizer()
        beepMuter?.restore()
        voicePlayer?.release()
        voicePlayer = null
        networkCallback?.let {
            try {
                val cm = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                cm.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        _binding = null
    }

    // ───────────────────── UI Setup ─────────────────────

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onRetry = { msg ->
                if (msg.isFailed) {
                    viewModel.sendText(msg.text, msg.id)
                }
            },
            onReport = { msg ->
                com.assan.finance.ai.AiContentReporter.showReportDialog(
                    requireActivity(), com.assan.finance.ai.AiContentReporter.SOURCE_HOME, msg.text
                )
            }
        )
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private var isTextMode = false

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnReportAnswer.setOnClickListener {
            // Latest answer, typed or spoken
            val last = viewModel.messages.value
                ?.lastOrNull { !it.isUser && it.text != VoiceChatViewModel.TYPING }
            com.assan.finance.ai.AiContentReporter.showReportDialog(
                requireActivity(), com.assan.finance.ai.AiContentReporter.SOURCE_HOME, last?.text
            )
        }

        // ── Text Mode: detect typing ──
        binding.etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    isTextMode = false
                    binding.btnAction.setImageResource(R.drawable.ic_mic)
                } else {
                    isTextMode = true
                    binding.btnAction.setImageResource(android.R.drawable.ic_menu_send)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // ── Main Action Button: Mic or Send ──
        binding.btnAction.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (isTextMode && text.isNotEmpty() && !isVoiceModeActive) {
                // TEXT MODE: send text, no voice
                viewModel.sendText(text)
                binding.etMessage.text.clear()
            } else if (isVoiceModeActive) {
                // IN VOICE MODE: the action button ends the voice conversation
                exitVoiceMode()
            } else {
                // VOICE MODE: open voice overlay
                enterVoiceMode()
            }
        }



        // ── Tap on voice bar: interrupt AI speaking → start listening ──
        binding.layoutVoiceRecording.setOnClickListener {
            if (voicePlayer?.isSpeaking == true) {
                voicePlayer?.stop()
                listenForNextQuestion()        // tap while it speaks = interrupt and ask again
            } else if (isWaitingForSend && (accumulatedText.isNotBlank() || lastPartialResult.isNotBlank())) {
                onUserTappedSend()             // tap while listening = send now
            }
        }
    }

    // ───────────────────── Voice Mode State Machine ─────────────────────

    /**
     * Enter voice mode: hide text input, show voice overlay, start listening.
     */
    private fun enterVoiceMode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        isVoiceModeActive = true
        isWaitingForSend = true
        accumulatedText = ""
        lastPartialResult = ""
        userHasSpoken = false

        binding.layoutInputText.visibility = View.GONE
        binding.layoutVoiceRecording.visibility = View.VISIBLE
        binding.btnAction.setImageResource(R.drawable.ic_close) // ends the voice conversation
        updateVoiceHint()
        startListening()
    }

    private fun updateVoiceHint() {
        binding.tvVoiceStatus.text = getString(R.string.str_listening)
    }

    /**
     * Exit voice mode: stop everything, show text input.
     */
    private fun exitVoiceMode() {
        beepMuter?.restore()
        isVoiceModeActive = false
        isWaitingForSend = false
        silenceHandler.removeCallbacksAndMessages(null)
        destroySpeechRecognizer()
        voicePlayer?.stop()
        accumulatedText = ""
        lastPartialResult = ""

        binding.layoutVoiceRecording.visibility = View.GONE
        binding.layoutInputText.visibility = View.VISIBLE
        binding.btnAction.setImageResource(R.drawable.ic_mic)
        binding.ivRecordingDot.clearAnimation()
    }

    /**
     * STATE: Listening
     * Start speech recognition. Show blinking red dot + "Listening..."
     */
    private fun startListening() {
        if (!isVoiceModeActive) return

        userHasSpoken = false
        lastPartialResult = ""
        silenceHandler.removeCallbacksAndMessages(null)

        updateVoiceHint()
        startDotBlink()

        // FIX: reuse one recognizer while listening. Destroying + recreating it on every restart unbound
        // Google's speech service under the new instance, which then failed with error 11 ("disconnected")
        // and, after a few, voice mode switched off by itself. It is only destroyed when listening ends.
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(CurrentRecognizerListener(recognizerToken))
        } else {
            try { speechRecognizer?.cancel() } catch (_: Exception) {}
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // A natural ~1.5 s pause ends the question (then it is sent automatically)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            // Android 14+: recognise both Urdu and English speech
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, arrayListOf("ur-PK", "en-US"))
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
            }
        }

        // Silence the recognizer's start/stop tone for the whole listening phase (it restarts often)
        beepMuter?.mute()
        speechRecognizer?.startListening(intent)
    }

    private fun restartListeningForMoreInput() {
        val partial = lastPartialResult.trim()
        if (partial.isNotBlank()) {
            accumulatedText = (accumulatedText + " $partial").trim()
            lastPartialResult = ""
        }
        if (isVoiceModeActive && isWaitingForSend) {
            startListening()
        }
    }

    private fun onUserTappedSend() {
        val partial = lastPartialResult.trim()
        if (partial.isNotBlank()) {
            accumulatedText = (accumulatedText + " $partial").trim()
        }
        val finalText = com.assan.finance.ai.AsaanAiEngine.normalizeSpokenText(accumulatedText)
        
        silenceHandler.removeCallbacksAndMessages(null)
        isWaitingForSend = false
        destroySpeechRecognizer()
        beepMuter?.restore() // the answer will be spoken: sound back on

        if (finalText.isEmpty()) {
            exitVoiceMode()
            return
        }

        binding.tvVoiceStatus.text = getString(R.string.str_thinking)
        binding.ivRecordingDot.clearAnimation()
        viewModel.sendVoice(finalText)
    }

    /**
     * Kept for backwards compatibility if needed
     */
    private fun finishListeningAndProcess() {
        onUserTappedSend()
    }

    /**
     * Called when AI finishes speaking (TTS onDone or MediaPlayer onCompletion).
     * Auto-restarts listening.
     */
    private fun onAiFinishedSpeaking() {
        if (isVoiceModeActive && _binding != null) {
            listenForNextQuestion()
        }
    }

    /** Continuous conversation: after each spoken answer, listen for the next question. */
    private fun listenForNextQuestion() {
        if (!isVoiceModeActive) return
        isWaitingForSend = true
        accumulatedText = ""
        lastPartialResult = ""
        startListening()
    }

    // ───────────────────── SpeechRecognizer Listener ─────────────────────

    /**
     * FIX: a destroyed recognizer can still deliver a late onError (e.g. 11 "server disconnected" or
     * 5 "client"). Those were counted as new failures, each restart produced another one, and after 3
     * voice mode switched itself off ("mic closes by itself"). Only the current recognizer is listened to.
     */
    private var recognizerToken = 0

    private inner class CurrentRecognizerListener(private val token: Int) : RecognitionListener {
        private fun live() = token == recognizerToken && _binding != null
        override fun onReadyForSpeech(params: Bundle?) { if (live()) voiceRecognitionListener.onReadyForSpeech(params) }
        override fun onBeginningOfSpeech() { if (live()) voiceRecognitionListener.onBeginningOfSpeech() }
        override fun onRmsChanged(rmsdB: Float) { if (live()) voiceRecognitionListener.onRmsChanged(rmsdB) }
        override fun onBufferReceived(buffer: ByteArray?) { if (live()) voiceRecognitionListener.onBufferReceived(buffer) }
        override fun onEndOfSpeech() { if (live()) voiceRecognitionListener.onEndOfSpeech() }
        override fun onError(error: Int) { if (live()) voiceRecognitionListener.onError(error) else Log.d("VoiceChat", "ignored stale onError: $error") }
        override fun onResults(results: Bundle?) { if (live()) voiceRecognitionListener.onResults(results) }
        override fun onPartialResults(partialResults: Bundle?) { if (live()) voiceRecognitionListener.onPartialResults(partialResults) }
        override fun onEvent(eventType: Int, params: Bundle?) { if (live()) voiceRecognitionListener.onEvent(eventType, params) }
    }

    private val voiceRecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceChat", "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d("VoiceChat", "onBeginningOfSpeech")
            // Reset silence timer only if already speaking (noise ignore)
            if (userHasSpoken) {
                silenceHandler.removeCallbacks(silenceRunnable)
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (rmsdB > 2.0f && userHasSpoken) {
                // User is still speaking → reset silence timer
                silenceHandler.removeCallbacks(silenceRunnable)
                silenceHandler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d("VoiceChat", "onEndOfSpeech")
            // Android's recognizer thinks user stopped. Start our 2s timer as safety.
            if (userHasSpoken) {
                silenceHandler.removeCallbacks(silenceRunnable)
                silenceHandler.postDelayed(silenceRunnable, 500) // shorter delay since recognizer already detected end
            }
        }

        override fun onError(error: Int) {
            Log.d("VoiceChat", "onError: $error")
            silenceHandler.removeCallbacks(silenceRunnable)

            if (!isVoiceModeActive) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // User paused — commit partial and restart so they can keep speaking.
                    // A short gap: recreating the recognizer instantly made the speech service
                    // disconnect (error 11) and, after a few of those, voice mode switched off.
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isVoiceModeActive && isWaitingForSend && _binding != null) restartListeningForMoreInput()
                    }, 400)
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isVoiceModeActive && isWaitingForSend) startListening()
                    }, 500)
                }
                else -> {
                    // FIX: fatal errors (no permission, no network, language not available, no
                    // speech service on the device) used to restart every 500 ms forever — muting
                    // and unmuting the phone's media volume each time. Stop and tell the user.
                    // Disconnected / client errors are usually temporary (speech service restarting):
                    // retry after 1 s; only give up after several in a row or without permission.
                    consecutiveSpeechErrors++
                    if (consecutiveSpeechErrors >= 5 ||
                        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        consecutiveSpeechErrors = 0
                        if (_binding != null) {
                            exitVoiceMode()
                            Toast.makeText(requireContext(), "Voice input is not available right now. Please type your question.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isVoiceModeActive && isWaitingForSend && _binding != null) startListening()
                        }, 1000)
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d("VoiceChat", "onResults")
            silenceHandler.removeCallbacks(silenceRunnable)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                lastPartialResult = matches[0]
                userHasSpoken = true
            }

            if (accumulatedText.isNotBlank() || lastPartialResult.isNotBlank()) {
                onUserTappedSend() // finished speaking -> send automatically
            } else {
                restartListeningForMoreInput()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partial.isNullOrEmpty() && partial[0].isNotBlank()) {
                lastPartialResult = partial[0]
                userHasSpoken = true
                consecutiveSpeechErrors = 0
                if (isVoiceModeActive) {
                    binding.tvVoiceStatus.text = "${getString(R.string.str_listening)} ${lastPartialResult.takeLast(28)}"
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ───────────────────── Observe ViewModel ─────────────────────

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            // TEXT MODE: only show non-voice messages in chat
            val visibleMessages = messages.filter { !it.isVoiceMessage }
            chatAdapter.submitList(visibleMessages)
            if (visibleMessages.isNotEmpty()) {
                binding.rvChat.scrollToPosition(visibleMessages.size - 1)
            }

            // VOICE MODE: handle AI voice reply
            if (messages.isNotEmpty()) {
                val lastMsg = messages.last()
                if (!lastMsg.isUser && lastMsg.isVoiceMessage && lastMsg.text != VoiceChatViewModel.TYPING && !lastMsg.hasBeenPlayed) {
                    lastMsg.hasBeenPlayed = true
                    if (isVoiceModeActive) speakText(lastMsg.text)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.etMessage.hint = getString(R.string.str_thinking)
                if (isVoiceModeActive) {
                    binding.tvVoiceStatus.text = getString(R.string.str_thinking)
                    binding.ivRecordingDot.clearAnimation()
                }
            } else {
                binding.etMessage.hint = "Ask Asaan Assistant..."
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null && !isVoiceModeActive) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
            // In voice mode: errors are silently retried by ViewModel.
            // If all retries fail, auto-restart listening.
            if (error != null && isVoiceModeActive) {
                startListening()
            }
        }
    }

    // ───────────────────── Audio Playback ─────────────────────

    /** Speaks the answer (natural Gemini voice, phone TTS as fallback). Status stays "Thinking..." until audio starts. */
    private fun speakText(text: String) {
        val player = voicePlayer
        if (text.isBlank() || player == null) {
            onAiFinishedSpeaking()
            return
        }
        player.speak(text, object : com.assan.finance.ai.VoiceReplyPlayer.Listener {
            override fun onSpeakingStarted() {
                if (isVoiceModeActive && _binding != null) {
                    binding.tvVoiceStatus.text = getString(R.string.str_speaking)
                    binding.ivRecordingDot.clearAnimation()
                }
            }
            override fun onSpeakingDone() {
                onAiFinishedSpeaking()
            }
        })
    }

    // ───────────────────── Helpers ─────────────────────

    private fun startDotBlink() {
        binding.ivRecordingDot.clearAnimation()
        val blink = android.view.animation.AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            interpolator = android.view.animation.LinearInterpolator()
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode = android.view.animation.Animation.REVERSE
        }
        binding.ivRecordingDot.startAnimation(blink)
    }

    private fun destroySpeechRecognizer() {
        recognizerToken++ // any late callback from the old recognizer is now ignored
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun setupNetworkListener() {
        val connectivityManager = requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                activity?.runOnUiThread {
                    viewModel.retryFailedMessages()
                }
            }
        }
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }
}
