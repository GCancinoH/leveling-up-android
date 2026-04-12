package com.gcancino.levelingup.domain.logic

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Date

object DateUtils {
    fun calculateAge(birthDate: Date): Int {
        val birthLocalDate = birthDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val now = LocalDate.now()
        return Period.between(birthLocalDate, now).years
    }
}