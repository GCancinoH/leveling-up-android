package com.gcancino.levelingup.core.voiceParser

import android.content.Context
import com.gcancino.levelingup.core.IVoiceToTextParser
import com.gcancino.levelingup.domain.models.VoiceToTextParserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

class OfflineVoiceParser(
    private val context: Context,
    private val model: Model
) : IVoiceToTextParser {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    override val state: StateFlow<VoiceToTextParserState> = _state.asStateFlow()

    private var speechService: SpeechService? = null
    private var onCommandReceived: ((String) -> Unit)? = null

    override fun startContinuousListening(
        languageCode: String,
        onCommand: (String) -> Unit
    ) {
        onCommandReceived = onCommand
        val recognizer = Recognizer(model, 16000.0f, "[\"start\", \"stop\", \"pause\", \"resume\"]")
        speechService = SpeechService(recognizer, 16000.0f)

        speechService?.startListening(object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                hypothesis?.let {
                    try {
                        val json = JSONObject(it)
                        val text = json.optString("partial")
                        if (text.isNotBlank()) {
                            _state.update { state -> state.copy(partialText = text, spokenText = text) }

                            parseCommand(text)?.let { command ->
                                _state.update { state -> state.copy(lastCommand = command) }
                                onCommandReceived?.invoke(command)
                            }
                        }
                    } catch (e: Exception) {
                        _state.update { state -> state.copy(error = e.message) }
                    }
                }
            }

            override fun onResult(result: String?) {
                result?.let {
                    try {
                        val json = JSONObject(it)
                        val text = json.optString("text")
                        _state.update { state -> state.copy(spokenText = text) }

                        parseCommand(text)?.let { command ->
                            _state.update { state -> state.copy(lastCommand = command) }
                            onCommandReceived?.invoke(command)
                        }
                    } catch (e: Exception) {
                        _state.update { state -> state.copy(error = e.message) }
                    }
                }
            }

            override fun onFinalResult(hypothesis: String?) {
                // Final confirmed transcription
                hypothesis?.let {
                    try {
                        val json = JSONObject(it)
                        val text = json.optString("text")
                        _state.update { state -> state.copy(spokenText = text) }

                        parseCommand(text)?.let { command ->
                            _state.update { state -> state.copy(lastCommand = command) }
                            onCommandReceived?.invoke(command)
                        }
                    } catch (e: Exception) {
                        _state.update { state -> state.copy(error = e.message) }
                    }
                }
            }

            override fun onError(exception: Exception?) {
                _state.update { state ->
                    state.copy(error = exception?.message ?: "Unknown error")
                }
            }

            override fun onTimeout() {
                _state.update { state -> state.copy(error = "Timeout while listening") }
            }
        })
    }

    override fun stopContinuousListening() {
        speechService?.stop()
        speechService = null
        onCommandReceived = null
    }

    override fun cleanup() {
        speechService?.shutdown()
        speechService = null
    }

    // --- Command parser ---
    private fun parseCommand(text: String): String? {
        val normalized = text.lowercase().trim()
        return when {
            normalized.contains("start") -> "START"
            normalized.contains("stop") -> "STOP"
            normalized.contains("pause") -> "PAUSE"
            normalized.contains("resume") -> "RESUME"
            else -> null
        }
    }
}