package com.gcancino.levelingup.presentation.player.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.core.SyncState
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import com.gcancino.levelingup.domain.repositories.DailyTasksRepository
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val bodyDataRepository: BodyDataRepository,
    private val dailyTasksRepository: DailyTasksRepository,
    private val identityRepository: IdentityRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun syncAll() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing

            // Corre todos en paralelo
            val results = listOf(
                async { bodyDataRepository.syncUnsynced() },
                async { dailyTasksRepository.syncUnsynced() },
                async { identityRepository.syncUnsynced() }
            ).awaitAll()

            val error = results.filterIsInstance<Resource.Error<*>>().firstOrNull()
            _syncState.value = if (error != null) {
                SyncState.Error(error.message ?: "Sync failed")
            } else {
                SyncState.Success
            }

            // Reset a Idle después de 3 segundos
            if (_syncState.value is SyncState.Success) {
                delay(3000)
                _syncState.value = SyncState.Idle
            }
        }
    }
}