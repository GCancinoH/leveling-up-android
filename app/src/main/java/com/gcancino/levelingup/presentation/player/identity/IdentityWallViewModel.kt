package com.gcancino.levelingup.presentation.player.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.data.local.database.dao.WeeklyReportDao
import com.gcancino.levelingup.domain.models.identity.IdentityScore
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class IdentityWallViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val weeklyReportDao: WeeklyReportDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val uID get() = auth.currentUser?.uid ?: ""

    val profile = identityRepository
        .observeIdentityProfile(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Last 7 days score for the streak heatmap
    val recentScores = identityRepository
        .observeTodayScore(uID) // extend repo with 7-day history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IdentityScore.EMPTY)

    val weeklyReports = weeklyReportDao
        .observeRecent(uID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current streak from player streak
    val currentStreak = MutableStateFlow(0)
}