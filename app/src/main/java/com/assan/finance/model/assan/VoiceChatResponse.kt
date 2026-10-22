package com.assan.finance.model.assan

data class VoiceChatResponse(
    val user_text: String?,
    val ai_text: String?,
    val audio: String? // Base64 data URI: data:audio/mp3;base64,...
)

data class VoiceChatResult(
    val success: Boolean,
    val data: VoiceChatResponse?,
    val message: String? = null
)
