package com.gcancino.levelingup.domain.logic

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate
    fun yesterdayBoundaries(): Pair<Long, Long>  // start/end of yesterday
    fun dayBoundaries(date: LocalDate): Pair<Long, Long>
}

@Singleton
class RealTimeProvider @Inject constructor() : TimeProvider {

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun today(): LocalDate = LocalDate.now()

    override fun yesterdayBoundaries(): Pair<Long, Long> =
        dayBoundaries(LocalDate.now().minusDays(1))

    override fun dayBoundaries(date: LocalDate): Pair<Long, Long> {
        val zone  = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}