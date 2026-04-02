package com.gcancino.levelingup.ui.components.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.CalendarViewmodel
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun WeeklyCalendar(
    viewModel: CalendarViewmodel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val today = LocalDate.now()
    val state = rememberWeekCalendarState(
        startDate = viewModel.startMonth.atDay(1),
        endDate = viewModel.endMonth.atEndOfMonth(),
        firstVisibleWeekDate = today,
        firstDayOfWeek = viewModel.firstDayOfWeek
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            WeekHeader(viewModel.firstDayOfWeek)
            Spacer(modifier = Modifier.height(16.dp))
            WeekCalendar(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                dayContent = { day ->
                    DayCell(
                        day = day,
                        isSelected = day.date == selectedDate,
                        isToday = day.date == today,
                        onClick = {
                            viewModel.onSelectedDate(day.date)
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun WeekHeader(firstDayOfWeek: DayOfWeek) {
    val days = daysOfWeek(firstDayOfWeek)

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        days.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(3),
                    color = Color.Gray,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DayCell(
    day: WeekDay,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) Color(0xFF3A3A3A)
                    else Color.Transparent,
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(4.dp)
                    .background(
                        Color(0xFF3A7AFE),
                        RoundedCornerShape(2.dp)
                    )
            )
        } else {

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}