package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BodyCompositionBottomSheetViewModel @Inject constructor(

) : ViewModel() {
    private val _hasEntryToday = MutableStateFlow(false)
    val hasEntryToday: StateFlow<Boolean> = _hasEntryToday.asStateFlow()
}

