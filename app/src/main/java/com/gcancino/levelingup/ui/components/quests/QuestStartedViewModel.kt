package com.gcancino.levelingup.ui.components.quests

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.IVoiceToTextParser
import com.gcancino.levelingup.core.OnlineVoiceParser
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.core.voiceParser.OfflineVoiceParser
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
import org.vosk.Model
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class QuestStartedViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val application: Application,
    private val offlineModel: Model? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestStartedUiState())
    val uiState: StateFlow<QuestStartedUiState> = _uiState.asStateFlow()

    // Dynamic voice parser based on connectivity
    private var currentVoiceParser: IVoiceToTextParser? = null
    private val _voiceState = MutableStateFlow(VoiceToTextParserState())
    val voiceState: StateFlow<VoiceToTextParserState> = _voiceState.asStateFlow()

    private var timerJob: Job? = null
    private var tts: TextToSpeech? = null
    private var connectivityJob: Job? = null

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        initializeTextToSpeech()
        startConnectivityMonitoring()
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

    private fun startConnectivityMonitoring() {
        connectivityJob = viewModelScope.launch {
            while (true) {
                val wasOnline = _uiState.value.isOnline
                val isCurrentlyOnline = isNetworkAvailable()

                if (wasOnline != isCurrentlyOnline) {
                    _uiState.update { it.copy(isOnline = isCurrentlyOnline) }

                    // Switch voice parser if currently listening
                    if (_uiState.value.canRecord && currentVoiceParser != null) {
                        switchVoiceParser()
                    }
                }

                delay(5000) // Check every 5 seconds
            }
        }
    }

    private fun setupVoiceRecognition() {
        val isOnline = isNetworkAvailable()
        _uiState.update { it.copy(isOnline = isOnline) }

        // Clean up existing parser
        currentVoiceParser?.cleanup()

        currentVoiceParser = if (isOnline) {
            OnlineVoiceParser(application.baseContext)
        } else {
            // Check if offline model is available
            if (offlineModel != null) {
                OfflineVoiceParser(application.baseContext, offlineModel)
            } else {
                _uiState.update {
                    it.copy(error = "No internet connection and offline model not available")
                }
                return
            }
        }

        // Observe voice parser state
        viewModelScope.launch {
            currentVoiceParser?.state?.collect { voiceState ->
                _voiceState.update { voiceState }
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
        if (currentVoiceParser == null) {
            setupVoiceRecognition()
        }

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
        when (command.uppercase()) {
            "START" -> startTimer()
            "PAUSE" -> pauseTimer()
            "RESUME" -> resumeTimer()
            "STOP" -> stopTimer()
        }

        // Update UI with last command received
        _uiState.update { it.copy(lastVoiceCommand = command) }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
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

                val result = questRepository.updateQuestStatus(quest.id)

                when (result) {
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
                    else -> null
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

    // Manual switch for testing or user preference
    fun forceOfflineMode() {
        currentVoiceParser?.stopContinuousListening()
        if (offlineModel != null) {
            currentVoiceParser = OfflineVoiceParser(application.baseContext, offlineModel)
            if (_uiState.value.canRecord) {
                startVoiceListening()
            }
            _uiState.update { it.copy(isOnline = false, connectionMessage = "Forced offline mode") }
        }
    }

    fun forceOnlineMode() {
        if (isNetworkAvailable()) {
            currentVoiceParser?.stopContinuousListening()
            currentVoiceParser = OnlineVoiceParser(application.baseContext)
            if (_uiState.value.canRecord) {
                startVoiceListening()
            }
            _uiState.update { it.copy(isOnline = true, connectionMessage = "Forced online mode") }
        } else {
            _uiState.update { it.copy(error = "Cannot switch to online mode: No internet connection") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentVoiceParser?.stopContinuousListening()
        currentVoiceParser?.cleanup()
        timerJob?.cancel()
        connectivityJob?.cancel()
        tts?.shutdown()
    }
}

/*class QuestStartedViewModel(
    val voiceParser: VoiceToTextParser,
    val questRepository: QuestRepositoryImpl,
    val application: Application
) : ViewModel() {
    private val _uiState = MutableStateFlow(QuestStartedUiState())
    val uiState: StateFlow<QuestStartedUiState> = _uiState.asStateFlow()

    private val _voiceState = voiceParser.state
    val voiceState: StateFlow<VoiceToTextParserState> = _voiceState

    private var timerJob: Job? = null
    private var tts: TextToSpeech? = null

    init {

    }



    fun requestMicPermission() { /* TODO() */  }

    fun onPermissionResult(isGranted: Boolean) {
        _uiState.update { it.copy(canRecord = isGranted) }

        if (isGranted) {
            startVoiceListening()
        }
    }

    private fun startVoiceListening() {
        viewModelScope.launch {
            try {
                voiceParser.startContinuousListening("en-US") { command ->
                    when (command.uppercase()) {
                        "START" -> startTimer()
                        "PAUSE" -> pauseTimer()
                        "RESUME" -> resumeTimer()
                        "STOP" -> stopTimer()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Voice listening failed: ${e.message}")
                }
            }
        }
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

        // Keep screen on
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

                // Update quest status to completed
                val result = questRepository.updateQuestStatus(quest.id)

                when (result) {
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
                    else -> null
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

        // Listen for state changes to provide voice feedback
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

                // Reset notification flag when appropriate
                if (state.timeElapsed == 0L || (state.targetTime?.let {
                        state.timeElapsed < (it * 0.8)
                    } == true)) {
                    _uiState.update { it.copy(hasNotifiedNearTarget = false) }
                }
            }
        }
    }

    private fun keepScreenOn(keepOn: Boolean) {
        // This needs to be handled in the composable with window flags
        _uiState.update { it.copy(keepScreenOn = keepOn) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        voiceParser.stopContinuousListening()
        timerJob?.cancel()
        tts?.shutdown()
    }

}*/

data class QuestStartedUiState(
    val quest: Quests? = null,
    val timeElapsed: Long = 0L,
    val targetTime: Long? = null,
    val isTimerRunning: Boolean = false,
    val canRecord: Boolean = false,
    val isOnline: Boolean = true,
    val isNearTarget: Boolean = false,
    val isOverTarget: Boolean = false,
    val hasNotifiedNearTarget: Boolean = false,
    val isSaving: Boolean = false,
    val questSaved: Boolean = false,
    val keepScreenOn: Boolean = false,
    val lastVoiceCommand: String? = null,
    val connectionMessage: String? = null,
    val error: String? = null
)

