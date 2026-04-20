package com.gcancino.levelingup.core

import android.content.Context
import com.gcancino.levelingup.domain.models.VoiceToTextParserState
import kotlinx.coroutines.flow.StateFlow

interface IVoiceToTextParser {
    val state: StateFlow<VoiceToTextParserState>

    fun startContinuousListening(
        languageCode: String = "en-US",
        onCommand: (String) -> Unit
    )

    fun stopContinuousListening()
    fun cleanup()
}

class VoiceToTextParser(
    private val context: Context,
    override val state: StateFlow<VoiceToTextParserState>,
) : IVoiceToTextParser {
    override fun startContinuousListening(
        languageCode: String,
        onCommand: (String) -> Unit
    ) {
        throw UnsupportedOperationException("VoiceToTextParser is a stub. Use OnlineVoiceParser or OfflineVoiceParser.")
    }

    override fun stopContinuousListening() {
        throw UnsupportedOperationException("VoiceToTextParser is a stub. Use OnlineVoiceParser or OfflineVoiceParser.")
    }

    override fun cleanup() {
        throw UnsupportedOperationException("VoiceToTextParser is a stub. Use OnlineVoiceParser or OfflineVoiceParser.")
    }


}