package com.assan.finance.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.assan.finance.ai.AsaanAiEngine
import com.assan.finance.ai.AsaanKnowledge

import kotlinx.coroutines.launch
import okhttp3.MultipartBody

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val audioBase64: String? = null,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val isVoiceMessage: Boolean = false,
    val isFailed: Boolean = false,
    var hasBeenPlayed: Boolean = false
)

/**
 * Home screen "Asaan Assistant".
 *  - Text mode: typed question -> text answer (chat bubbles).
 *  - Voice mode: spoken question -> spoken answer only (no bubbles; the Fragment speaks it).
 * Both modes share one conversation memory, and the model thinks before answering.
 */
class VoiceChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Conversation memory sent to the model (text and voice turns together). */
    private val history = mutableListOf<AsaanAiEngine.Turn>()

    companion object {
        const val TYPING = "Typing..."

        private val IDENTITY = """
            You are "Asaan Assistant", the official AI assistant inside the Asaan Finance mobile app by NRSP (Pakistan).
            You help users understand the app and Asaan Financing: how to apply, required documents, CNIC scanning,
            durations, the ledger and repayments, notifications, profile, settings and support.
        """.trimIndent()

        private fun systemPrompt(voice: Boolean) = listOf(
            IDENTITY,
            AsaanKnowledge.APP_FACTS,
            AsaanKnowledge.THINKING_RULES,
            AsaanKnowledge.COMMON_RULES,
            if (voice) AsaanKnowledge.VOICE_RULES else AsaanKnowledge.TEXT_RULES
        ).joinToString("\n\n")
    }

    fun retryFailedMessages() {
        val currentList = _messages.value.orEmpty()
        val failedMessages = currentList.filter { it.isUser && it.isFailed && !it.isVoiceMessage }
        for (msg in failedMessages) {
            sendText(msg.text, msg.id)
        }
    }

    /** TEXT -> TEXT */
    fun sendText(text: String, retryId: String? = null) {
        _isLoading.value = true
        _error.value = null

        val currentList = _messages.value.orEmpty().toMutableList()
        val msgId = retryId ?: java.util.UUID.randomUUID().toString()
        if (retryId == null) {
            currentList.add(ChatMessage(id = msgId, text = text, isUser = true))
        } else {
            val index = currentList.indexOfFirst { it.id == retryId }
            if (index != -1) currentList[index] = currentList[index].copy(isFailed = false)
        }
        currentList.add(ChatMessage(text = TYPING, isUser = false)) // animated "Thinking..." bubble
        _messages.value = currentList

        viewModelScope.launch {
            val reply = answer(text, voice = false)
            val finalList = _messages.value.orEmpty().toMutableList()
            if (finalList.isNotEmpty() && finalList.last().text == TYPING) finalList.removeAt(finalList.size - 1)
            finalList.add(ChatMessage(text = reply, isUser = false))
            _messages.value = finalList
            _isLoading.value = false
        }
    }

    /**
     * VOICE -> VOICE. The recognised speech is answered for speaking; no chat bubbles are added.
     * The Fragment plays every new message with isVoiceMessage = true.
     */
    fun sendVoice(text: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val reply = answer(text, voice = true)
            val finalList = _messages.value.orEmpty().toMutableList()
            finalList.add(ChatMessage(text = reply, isUser = false, isVoiceMessage = true))
            _messages.value = finalList
            _isLoading.value = false
        }
    }

    /** Thinking model first, then the backend, then the offline rule-based answers. */
    private suspend fun answer(userText: String, voice: Boolean): String {
        val modelReply = AsaanAiEngine.generate(
            systemPrompt = systemPrompt(voice),
            history = history.toList(),
            userText = userText,
            thinkingBudget = if (voice) 512 else 1024
        )
        val reply = when {
            !modelReply.isNullOrBlank() -> modelReply
            else -> callBackend(userText) ?: generateFallbackResponse(userText)
        }.let { AsaanAiEngine.polishReply(sanitizeFinancingText(it)) }

        if (com.assan.finance.BuildConfig.DEBUG) {
            Log.d("AssistantChat", "[${if (voice) "VOICE" else "TEXT"}] Q: $userText")
            Log.d("AssistantChat", "[${if (voice) "VOICE" else "TEXT"}] A: $reply")
        }
        history.add(AsaanAiEngine.Turn(true, userText))
        history.add(AsaanAiEngine.Turn(false, reply))
        if (history.size > 24) history.subList(0, history.size - 24).clear()
        return reply
    }

    /** Single attempt at the backend AI endpoint (used only when every model failed). */
    private suspend fun callBackend(userText: String): String? = try {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("text", userText + "\n\n" + systemPrompt(false))
            .build()
        val response = com.assan.finance.network.assan.AssanRetrofitClient.apiService.processVoiceChatText(body)
        if (response.isSuccessful && response.body()?.success == true) response.body()?.data?.ai_text else null
    } catch (e: Exception) {
        Log.w("VoiceChatVM", "Backend AI failed: ${e.message}")
        null
    }

    private fun sanitizeFinancingText(text: String): String {
        return text
            .replace(Regex("(?i)\\bloan application\\b"), "Financing Application")
            .replace(Regex("(?i)\\bdarkhwast loan\\b"), "Darkhwast Financing")
            .replace(Regex("(?i)\\bloan categories\\b"), "Financing Categories")
            .replace(Regex("(?i)\\bloan category\\b"), "Financing Category")
            .replace(Regex("(?i)\\bloan amount\\b"), "Financing Amount")
            .replace(Regex("(?i)\\bloans\\b"), "Financing")
            .replace(Regex("(?i)\\bloan\\b"), "Financing")
            .replace(Regex("(?i)\\bqarz\\b"), "Financing")
            .replace(Regex("(?i)\\bkarz\\b"), "Financing")
            .replace("**", "")
    }

    private fun isEnglishText(text: String): Boolean = AsaanAiEngine.looksEnglish(text)

    private fun isOutOfScopeQuery(lower: String): Boolean {
        val outOfScopeKeywords = listOf(
            "weather", "mausam", "barish", "rain", "temperature", "forecast",
            "cricket", "match", "score", "football", "babar", "virat", "ipl", "psl", "world cup",
            "president", "prime minister", "wazir e azam", "wazir-e-azam", "imran khan", "nawaz", "modi", "trump",
            "biden", "election", "siyasat", "politics", "vote",
            "recipe", "khana", "biryani", "cook", "cooking", "restaurant", "tea", "chai",
            "code", "coding", "python", "javascript", "java", "c++", "html", "css", "programming", "developer",
            "movie", "song", "film", "actor", "actress", "drama", "latifa", "joke", "shairi", "poetry",
            "dance", "who is the president", "capital of", "2+2", "story", "kahani"
        )
        // Whole words/phrases only, so "branch code", "payment history" etc. are not blocked.
        return outOfScopeKeywords.any { kw ->
            if (kw == "c++" || kw == "2+2") lower.contains(kw)
            else Regex("(?<![a-z])" + Regex.escape(kw) + "(?![a-z])").containsMatchIn(lower)
        } && !Regex("(?<![a-z])(branch code|promo code|account|financing|installment|qist)(?![a-z])").containsMatchIn(lower)
    }

    /** Offline answers, used only when no AI service is reachable. */
    private fun generateFallbackResponse(userText: String): String {
        val lower = userText.lowercase(Locale.ROOT).trim()
        val english = isEnglishText(userText)

        if (isOutOfScopeQuery(lower)) {
            return if (english) {
                "I am Asaan Assistant. I can only help with the Asaan Finance app and its financing services. How can I help you with your financing today?"
            } else {
                "Main Asaan Assistant hoon. Main sirf Asaan Finance app aur financing ke mutaliq madad kar sakta hoon. Financing ke baare mein aap kya jaanna chahte hain?"
            }
        }

        if (lower.contains("salam") || lower.contains("assalam") || lower.contains("asalam") || lower.contains("slam") || lower.contains("adaab")) {
            return "Walaikum Assalam! Main Asaan Assistant hoon. Financing application, documents ya account ke baare mein main aap ki kya madad kar sakta hoon?"
        }

        if (Regex("\\b(hi|hello|hey|good morning|good afternoon|good evening)\\b").containsMatchIn(lower)) {
            return if (english) "Hello! I am Asaan Assistant. How can I help you with your financing today?"
            else "Hello! Main Asaan Assistant hoon. Asaan Financing mein main aap ki kya madad kar sakta hoon?"
        }

        if (lower.contains("kaise ho") || lower.contains("kese ho") || lower.contains("kya haal") || lower.contains("kia hal") || lower.contains("how are you")) {
            return if (english) "I am well, thank you! How can I help you with Asaan Finance today?"
            else "Alhamdulillah, main theek hoon! Farmayein, Asaan Finance ke baare mein aap ko kya maloomat chahiye?"
        }

        if (lower.contains("apply") || lower.contains("darkhwast") || lower.contains("chahiye") || lower.contains("chahye") || lower.contains("lena") || lower.contains("tarika") || lower.contains("tareeqa") || lower.contains("how to") || lower.contains("loan")) {
            return if (english) "To apply, tap 'Financing Application' on the Home screen, scan your original CNIC (front and back) and complete the form."
            else "Financing ke liye Home screen par 'Financing Application' par tap karein, apna asli CNIC scan karein aur form mukammal karein."
        }

        if (lower.contains("document") || lower.contains("kaghaz") || lower.contains("cnic") || lower.contains("requirement")) {
            return if (english) "You need your original CNIC, an active mobile number, your bank account details (bank, account number, branch code), a recent photo and basic business or income details."
            else "Aap ko asli CNIC, active mobile number, bank account ki maloomat (bank, account number, branch code), ek taza tasweer aur karobar ya aamdani ki bunyadi maloomat chahiye."
        }

        if (lower.contains("kitna") || lower.contains("kitni") || lower.contains("amount") || lower.contains("limit") || lower.contains("raqam") || lower.contains("how much")) {
            return if (english) "Financing of up to Rs. 150,000 is available; the exact limit depends on the purpose you choose. Select the purpose in the Financing Application to see the minimum and maximum amount."
            else "Rs. 150,000 tak financing dastiyab hai; durust had aap ke chunay gaye maqsad par munhasir hai. Financing Application mein maqsad chunein, wahin kam az kam aur zyada se zyada raqam nazar aa jayegi."
        }

        if (lower.contains("muddat") || lower.contains("duration") || lower.contains("mahine") || lower.contains("installment") || lower.contains("months")) {
            return if (english) "You can choose a repayment duration of 6, 12 or 18 months."
            else "Aap wapsi ki muddat 6, 12 ya 18 mahine chun sakte hain."
        }

        if (lower.contains("status") || lower.contains("ledger") || lower.contains("track") || lower.contains("repayment")) {
            return if (english) "You can track your financing and repayments in 'Financing Ledger' on the Home screen, and you will get notifications on status changes."
            else "Apni financing aur aqsaat Home screen par 'Financing Ledger' mein dekh sakte hain, aur status badalne par aap ko notification mil jayegi."
        }

        if (lower.contains("helpline") || lower.contains("contact") || lower.contains("rabta") || lower.contains("support")) {
            return if (english) "You can reach support by call or WhatsApp at ${com.assan.finance.utils.Constants.SUPPORT_PHONE}, email ${com.assan.finance.utils.Constants.SUPPORT_EMAIL}, or send feedback from Settings."
            else "Aap support se ${com.assan.finance.utils.Constants.SUPPORT_PHONE} par call ya WhatsApp, ya ${com.assan.finance.utils.Constants.SUPPORT_EMAIL} par email kar sakte hain, ya Settings se feedback bhej sakte hain."
        }

        if (lower.contains("shukriya") || lower.contains("thanks") || lower.contains("thank you") || lower.contains("meherbani")) {
            return if (english) "You are welcome! Feel free to ask anything else about Asaan Finance."
            else "Aap ka bohot shukriya! Koi aur sawal ho to zaroor poochein."
        }

        return if (english) {
            "I can help with the financing application, documents, CNIC scanning, durations and your ledger. What would you like to know?"
        } else {
            "Main financing application, documents, CNIC scanning, muddat aur ledger ke baare mein madad kar sakta hoon. Aap kya jaanna chahte hain?"
        }
    }
}
