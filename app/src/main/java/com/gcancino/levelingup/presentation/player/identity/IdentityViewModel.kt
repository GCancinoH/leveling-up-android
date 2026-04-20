package com.gcancino.levelingup.presentation.player.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.identity.DailyStandardEntry
import com.gcancino.levelingup.domain.models.identity.IdentityProfile
import com.gcancino.levelingup.domain.models.identity.IdentityScore
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val TAG = "IdentityViewModel"
    private val uID get() = auth.currentUser?.uid ?: ""

    // Perfil de identidad
    val identityProfile: StateFlow<IdentityProfile?> = identityRepository
        .observeIdentityProfile(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Score del día (reactivo — se actualiza cuando el usuario completa algo)
    val todayScore: StateFlow<IdentityScore> = identityRepository
        .observeTodayScore(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IdentityScore.EMPTY)

    // Entradas del día
    val todayEntries: StateFlow<List<DailyStandardEntry>> = identityRepository
        .observeTodayEntries(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingEntries: StateFlow<List<DailyStandardEntry>> = identityRepository
        .observePendingEntries(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // XP ganado (para mostrar animación)
    private val _xpEarned = MutableSharedFlow<Int>(replay = 0)
    val xpEarned: SharedFlow<Int> = _xpEarned.asSharedFlow()

    private val _levelUp = MutableSharedFlow<Int>(replay = 0)
    val levelUp: SharedFlow<Int> = _levelUp.asSharedFlow()

    // Completar estándar manualmente
    fun completeStandard(entryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = identityRepository.completeStandard(entryId, uID)) {
                is Resource.Success -> {
                    val xp = result.data ?: 0
                    Timber.tag(TAG).i("Estándar completado → +$xp XP")
                    _xpEarned.emit(xp)
                }
                is Resource.Error -> {
                    Timber.tag(TAG).e("completeStandard() falló: ${result.message}")
                }
                else -> Unit
            }
        }
    }

    fun saveIdentityProfile(
        statement: String,
        roles: List<Role>,
        standards: List<IdentityStandard>
    ) {}

    // Generar entradas del día si no existen
    // Llamado en init y cuando el dashboard se abre
    init {
        generateTodayEntriesIfNeeded()
    }

    fun generateTodayEntriesIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            identityRepository.generateTodayEntries(uID)
        }
    }
}