package com.gcancino.levelingup.presentation.player.dashboard.viewModels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewmodel : ViewModel() {
    private val today = LocalDate.now()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val firstDayOfWeek = DayOfWeek.SUNDAY

    val startMonth: YearMonth = YearMonth.now().minusYears(1)
    val endMonth: YearMonth = YearMonth.now()

    fun onSelectedDate(date: LocalDate) {
        if(!date.isAfter(today)) {
            _selectedDate.value = date
        }
    }
}