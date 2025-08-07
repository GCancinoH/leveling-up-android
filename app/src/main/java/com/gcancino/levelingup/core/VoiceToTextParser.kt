package com.gcancino.levelingup.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceToTextParser(
    private val context: Context
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state = _state.asStateFlow()

    private val recognizer: SpeechRecognizer? = SpeechRecognizer.createSpeechRecognizer(context)
    private var currentLanguageCode = "en-US"
    private var isContinuousListening = false
    private var onCommandReceived: ((String) -> Unit)? = null

    fun startContinuousListening(
        languageCode: String = "en-US",
        onCommand: (String) -> Unit
    ) {
        currentLanguageCode = languageCode
        onCommandReceived = onCommand
        isContinuousListening = true
        startListening()
    }

    fun stopContinuousListening() {
        isContinuousListening = false
        onCommandReceived = null
        stopListening()
    }

    private fun startListening() {
        _state.update { VoiceToTextParserState() }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(error = "Speech recognition not available")
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.setRecognitionListener(this)
        recognizer?.startListening(intent)

        _state.update {
            it.copy(isSpeaking = true)
        }
    }

    fun stopListening() {
        _state.update {
            it.copy(isSpeaking = false)
        }
        recognizer?.stopListening()
    }

    private fun parseCommand(spokenText: String): String? {
        val text = spokenText.lowercase().trim()
        return when {
            text.contains("start") -> "START"
            text.contains("stop") -> "STOP"
            text.contains("pause") -> "PAUSE"
            text.contains("resume") -> "RESUME"
            else -> null
        }
    }

    private fun restartListening() {
        if (isContinuousListening) {
            // Small delay to avoid rapid restarts
            Handler(Looper.getMainLooper()).postDelayed({
                if (isContinuousListening) {
                    startListening()
                }
            }, 500)
        }
    }

    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(p0: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _state.update {
            it.copy(isSpeaking = false)
        }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT) {
            return
        }

        _state.update {
            it.copy(
                error = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    else -> "Recognition error: $error"
                },
                isSpeaking = false
            )
        }

        // Restart listening if in continuous mode and error is recoverable
        if (isContinuousListening && error in listOf(
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_NO_MATCH
            )) {
            restartListening()
        }
    }

    override fun onEvent(p0: Int, p1: Bundle?) = Unit

    override fun onPartialResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { partialText ->
                _state.update {
                    it.copy(partialText = partialText)
                }

                // Check for commands in partial results for faster response
                parseCommand(partialText)?.let { command ->
                    onCommandReceived?.invoke(command)
                }
            }
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        _state.update {
            it.copy(error = null)
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { spokenText ->
                _state.update {
                    it.copy(
                        spokenText = spokenText,
                        lastCommand = parseCommand(spokenText)
                    )
                }

                // Process command
                parseCommand(spokenText)?.let { command ->
                    onCommandReceived?.invoke(command)
                }

                // Restart listening for continuous mode
                restartListening()
            }
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Update voice level for UI feedback
        _state.update {
            it.copy(voiceLevel = rmsdB)
        }
    }

    fun cleanup() {
        isContinuousListening = false
        recognizer?.destroy()
    }
}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val partialText: String = "",
    val lastCommand: String? = null,
    val isSpeaking: Boolean = false,
    val voiceLevel: Float = 0f,
    val error: String? = null
)