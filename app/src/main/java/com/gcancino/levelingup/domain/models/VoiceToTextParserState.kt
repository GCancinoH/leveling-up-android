package com.gcancino.levelingup.domain.models

data class VoiceToTextParserState(
    val spokenText: String = "",
    val partialText: String = "",
    val lastCommand: String? = null,
    val isSpeaking: Boolean = false,
    val voiceLevel: Float = 0f,
    val error: String? = null
)
