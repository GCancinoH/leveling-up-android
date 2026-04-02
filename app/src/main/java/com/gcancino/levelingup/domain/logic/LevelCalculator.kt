package com.gcancino.levelingup.domain.logic

import kotlin.math.floor
import kotlin.math.sqrt

object LevelCalculator {
    private const val BASE_XP = 100.0
    private const val MAX_LEVEL = 100
    const val POINTS_PER_LEVEL = 5

    /**
     * Calculates the level based on total XP.
     * Formula: L = sqrt(XP / 100) + 1
     */
    fun calculateLevel(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        val level = floor(sqrt(totalXp / BASE_XP)).toInt() + 1
        return level.coerceAtMost(MAX_LEVEL)
    }

    /**
     * Calculates the total XP required to reach a specific level.
     * Formula: TotalXP = 100 * (L - 1)^2
     */
    fun xpRequiredForLevel(level: Int): Int {
        if (level <= 1) return 0
        return (BASE_XP * (level - 1) * (level - 1)).toInt()
    }

    /**
     * Calculates progress within the current level (0.0 to 1.0).
     */
    fun calculateProgress(totalXp: Int): Float {
        val currentLevel = calculateLevel(totalXp)
        if (currentLevel >= MAX_LEVEL) return 1.0f

        val xpStartCurrent = xpRequiredForLevel(currentLevel)
        val xpStartNext = xpRequiredForLevel(currentLevel + 1)
        
        val xpInCurrentLevel = totalXp - xpStartCurrent
        val xpNeededForNextLevel = xpStartNext - xpStartCurrent
        
        return (xpInCurrentLevel.toFloat() / xpNeededForNextLevel.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns the XP remaining until the next level.
     */
    fun xpToNextLevel(totalXp: Int): Int {
        val currentLevel = calculateLevel(totalXp)
        if (currentLevel >= MAX_LEVEL) return 0
        val xpStartNext = xpRequiredForLevel(currentLevel + 1)
        return xpStartNext - totalXp
    }
}
