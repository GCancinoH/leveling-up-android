package com.gcancino.levelingup.presentation.player.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.data.local.database.dao.GeneratedQuestDao
import com.gcancino.levelingup.data.local.database.entities.identity.GeneratedQuestEntity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ActiveQuestViewModel @Inject constructor(
    private val questDao: GeneratedQuestDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val uID get() = auth.currentUser?.uid ?: ""

    val activeQuest: StateFlow<GeneratedQuestEntity?> = questDao
        .observeActive(uID)
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}