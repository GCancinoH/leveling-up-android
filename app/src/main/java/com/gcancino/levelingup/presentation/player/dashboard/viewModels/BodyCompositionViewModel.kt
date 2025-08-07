package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BodyCompositionBottomSheetViewModel(

) : ViewModel() {
    private val _hasEntryToday = MutableStateFlow(false)
    val hasEntryToday: StateFlow<Boolean> = _hasEntryToday.asStateFlow()
}

