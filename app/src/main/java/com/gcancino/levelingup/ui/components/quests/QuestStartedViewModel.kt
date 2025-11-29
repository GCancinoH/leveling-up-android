package com.gcancino.levelingup.ui.components.quests

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.core.di.VoiceParserFactory
import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.models.VoiceToTextParserState
import com.gcancino.levelingup.domain.repositories.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class QuestStartedViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val application: Application,
    private val voiceParserFactory: VoiceParserFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestStartedUiState())
    val uiState: StateFlow<QuestStartedUiState> = _uiState.asStateFlow()

    // Dynamic voice parser based on connectivity
    private var currentVoiceParser = voiceParserFactory.create()

    private var timerJob: Job? = null
    private var tts: TextToSpeech? = null
    private var connectivityJob: Job? = null
    private var voiceStateCollectorJob: Job? = null

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback: ConnectivityManager.NetworkCallback

    init {
        initializeTextToSpeech()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                _uiState.update { it.copy(isOnline = true) }
                switchVoiceParser()
            }

            override fun onLost(network: android.net.Network) {
                _uiState.update { it.copy(isOnline = false) }
                switchVoiceParser()
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        switchVoiceParser()
    }

    fun loadQuest(questId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = questRepository.getQuestByQuestID(questId)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                quest = result.data,
                                isLoading = false,
                                targetTime = result.data?.details?.targetTime?.toLong()
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load quest"
                            )
                        }
                    }

                    is Resource.Loading -> {
                        // Handle loading if needed
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Exception: ${e.message}"
                    )
                }
            }
        }
    }

    fun initializeQuest(quest: Quests) {
        _uiState.update { currentState ->
            currentState.copy(
                quest = quest,
                targetTime = quest.details?.targetTime?.toLong()
            )
        }
    }

    fun requestMicPermission() { /* TODO() */ }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(canRecord = isGranted) }

        if (isGranted) {
            setupVoiceRecognition()
            startVoiceListening()
        }
    }




    private fun setupVoiceRecognition() {
        // Clean up existing parser
        currentVoiceParser?.cleanup()

        currentVoiceParser = voiceParserFactory.create()

        if (currentVoiceParser == null) {
            _uiState.update {
                it.copy(error = "No voice parser available")
            }
            return
        }

        // Observe voice parser state
        voiceStateCollectorJob?.cancel()
        voiceStateCollectorJob = viewModelScope.launch {
            currentVoiceParser?.state?.collect { voiceState ->
                _uiState.update {
                    it.copy(
                        isSpeaking = voiceState.isSpeaking,
                        spokenText = voiceState.spokenText,
                        partialText = voiceState.partialText,
                        voiceLevel = voiceState.voiceLevel,
                        error = voiceState.error
                    )
                }
            }
        }
    }

    private fun switchVoiceParser() {
        val wasListening = currentVoiceParser != null

        // Stop current parser
        currentVoiceParser?.stopContinuousListening()

        // Setup new parser
        setupVoiceRecognition()

        // Restart listening if it was active
        if (wasListening && _uiState.value.canRecord) {
            startVoiceListening()
        }

        // Notify user of the switch
        val connectionType = if (_uiState.value.isOnline) "online" else "offline"
        _uiState.update {
            it.copy(connectionMessage = "Switched to $connectionType voice recognition")
        }

        // Clear message after a few seconds
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(connectionMessage = null) }
        }
    }

    private fun startVoiceListening() {
        viewModelScope.launch {
            try {
                currentVoiceParser?.startContinuousListening("en-US") { command ->
                    handleVoiceCommand(command)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Voice listening failed: ${e.message}")
                }
            }
        }
    }

    private fun handleVoiceCommand(command: String) {
        when (command.uppercase(Locale.getDefault())) {
            "START" -> startTimer()
            "PAUSE" -> pauseTimer()
            "RESUME" -> resumeTimer()
            "STOP" -> stopTimer()
        }

        // Update UI with last command received
        _uiState.update { it.copy(lastVoiceCommand = command) }
    }


    fun startTimer() {
        if (_uiState.value.isTimerRunning) return

        _uiState.update { it.copy(isTimerRunning = true, error = null) }

        timerJob = viewModelScope.launch {
            try {
                while (_uiState.value.isTimerRunning) {
                    delay(1000)
                    _uiState.update { currentState ->
                        val newElapsed = currentState.timeElapsed + 1
                        val targetTime = currentState.targetTime

                        currentState.copy(
                            timeElapsed = newElapsed,
                            isNearTarget = targetTime?.let {
                                newElapsed >= (it * 0.8) && newElapsed < it
                            } ?: false,
                            isOverTarget = targetTime?.let { newElapsed > it } ?: false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Timer error: ${e.message}", isTimerRunning = false)
                }
            }
        }

        keepScreenOn(true)
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isTimerRunning = false) }
        timerJob?.cancel()
        keepScreenOn(false)
    }

    fun resumeTimer() {
        if (!_uiState.value.isTimerRunning) {
            startTimer()
        }
    }

    fun stopTimer() {
        _uiState.update { it.copy(isTimerRunning = false) }
        timerJob?.cancel()
        keepScreenOn(false)
    }

    fun resetTimer() {
        stopTimer()
        _uiState.update {
            it.copy(
                timeElapsed = 0L,
                isNearTarget = false,
                isOverTarget = false,
                hasNotifiedNearTarget = false
            )
        }
    }

    fun saveQuestResults() {
        val currentState = _uiState.value
        val quest = currentState.quest ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }

                when (val result = questRepository.updateQuestStatus(quest.id)) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                questSaved = true
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                error = result.message ?: "Failed to save quest"
                            )
                        }
                    }

                    is Resource.Loading -> null
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Save failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun initializeTextToSpeech() {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        viewModelScope.launch {
            uiState.collect { state ->
                when {
                    state.isNearTarget && !state.hasNotifiedNearTarget -> {
                        _uiState.update { it.copy(hasNotifiedNearTarget = true) }
                        tts?.speak(
                            "Almost there! Keep going!",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "nearTargetNotification"
                        )
                    }

                    state.isOverTarget -> {
                        tts?.speak(
                            "You did it! Great job!",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "overTargetNotification"
                        )
                    }
                }

                if (state.timeElapsed == 0L || (state.targetTime?.let {
                        state.timeElapsed < (it * 0.8)
                    } == true)) {
                    _uiState.update { it.copy(hasNotifiedNearTarget = false) }
                }
            }
        }
    }

    private fun keepScreenOn(keepOn: Boolean) {
        _uiState.update { it.copy(keepScreenOn = keepOn) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissConnectionMessage() {
        _uiState.update { it.copy(connectionMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        currentVoiceParser?.stopContinuousListening()
        currentVoiceParser?.cleanup()
        timerJob?.cancel()
        connectivityJob?.cancel()
        tts?.shutdown()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

data class QuestStartedUiState(
    // Quest and Timer State
    val quest: Quests? = null,
    val timeElapsed: Long = 0L,
    val targetTime: Long? = null,
    val isTimerRunning: Boolean = false,
    val isNearTarget: Boolean = false,
    val isOverTarget: Boolean = false,
    val hasNotifiedNearTarget: Boolean = false,
    val isSaving: Boolean = false,
    val questSaved: Boolean = false,
    val keepScreenOn: Boolean = false,

    // Voice Recognition State
    val canRecord: Boolean = false,
    val isSpeaking: Boolean = false,
    val spokenText: String = "",
    val partialText: String = "",
    val voiceLevel: Float = 0f,
    val lastVoiceCommand: String? = null,

    // General UI State
    val isOnline: Boolean = true,
    val connectionMessage: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)
