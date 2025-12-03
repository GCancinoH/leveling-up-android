package com.gcancino.levelingup.domain.models

import android.os.Parcelable
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Quests(
    val id: String = "",
    val types: List<Improvement>? = null,
    val title: String? = null,
    val description: String? = null,
    val status: QuestStatus? = QuestStatus.NOT_STARTED,
    val date: Date = Date(),
    val startedDate: Date? = null,
    val finishedDate: Date? = null,
    val rewards: QuestRewards? = null,
    val details: QuestDetails? = null,
    val streak: QuestStreak? = null
) : Parcelable

enum class QuestStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class QuestType {
    ENDURANCE,
    STRENGTH,
    RECOVERY
}

@Parcelize
data class QuestRewards(
    val xp: Int? = null,
    val coins: Int? = null,
    val attributes: QuestAttributes? = null
) : Parcelable

@Parcelize
data class QuestAttributes(
    val strength: Int? = null,
    val intelligence: Int? = null,
    val endurance: Int? = null,
    val mobility: Int? = null,
    val health: Int? = null,
    val finesse: Int? = null,
) : Parcelable

@Parcelize
data class QuestDetails(
    val type: QuestType? = null,
    val progressionIncrement: Float? = null,

    // Endurance fields
    val targetTime: Int? = null,
    val currentTime: Int? = null,
    val targetDistance: Int? = null,
    val currentDistance: Int? = null,

    // Strength fields
    val targetReps: Int? = null,
    val currentReps: Int? = null,

    // Recovery fields
    val targetWater: Int? = null,
    val currentWater: Int? = null,
    val currentSleep: Int? = null,
    val targetSleep: Int? = null,
    val currentColdBaths: Int? = null,
    val targetColdBaths: Int? = null
) : Parcelable {
    // Helper methods to check quest type
    fun isEnduranceQuest() = type == QuestType.ENDURANCE
    fun isStrengthQuest() = type == QuestType.STRENGTH
    fun isRecoveryQuest() = type == QuestType.RECOVERY

    // Helper methods to get relevant fields based on type
    fun getEnduranceDetails() = if (isEnduranceQuest()) {
        EnduranceDetails(progressionIncrement, targetTime, currentTime, targetDistance, currentDistance)
    } else null

    fun getStrengthDetails() = if (isStrengthQuest()) {
        StrengthDetails(progressionIncrement, targetTime, currentTime, targetReps, currentReps)
    } else null

    fun getRecoveryDetails() = if (isRecoveryQuest()) {
        RecoveryDetails(progressionIncrement, targetWater, currentWater, currentSleep, targetSleep, currentColdBaths, targetColdBaths)
    } else null
}

// Optional: Helper data classes for type safety when working with specific quest types
@Parcelize
data class EnduranceDetails(
    val progressionIncrement: Float?,
    val targetTime: Int?,
    val currentTime: Int?,
    val targetDistance: Int?,
    val currentDistance: Int?
) : Parcelable

@Parcelize
data class StrengthDetails(
    val progressionIncrement: Float?,
    val targetTime: Int?,
    val currentTime: Int?,
    val targetReps: Int?,
    val currentReps: Int?
) : Parcelable

@Parcelize
data class RecoveryDetails(
    val progressionIncrement: Float?,
    val targetWater: Int?,
    val currentWater: Int?,
    val currentSleep: Int?,
    val targetSleep: Int?,
    val currentColdBaths: Int?,
    val targetColdBaths: Int?
) : Parcelable

@Parcelize
data class QuestStreak(
    val currentStreak: Int? = 0,
    val longestStreak: Int? = 0,
    val lastStreakDate: Date? = null
) : Parcelable

/*val dailyQuests: List<Quests> = listOf(
    Quests(
        id = "quest01",
        title = "Awakening of the Cold Warrior",
        description = "Face the Purification of Immovable Ice. This challenge shall forge your will and temper your spirit. With each immersion, the Frigid Baptism of Temperance purifies you, strenghens your Elemental Defense and clears your mind for crucial decisions.",
        types = listOf(Improvement.RECOVERY, Improvement.ENDURANCE),
        status = QuestStatus.NOT_STARTED,
        date = Date(),
        rewards = QuestRewards(
            xp = 10,
            coins = 2,
            attributes = QuestAttributes(
                endurance = 5,
                health = 5
            ),
        ),
        streak = QuestStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStreakDate = null
        ),
        details = QuestDetails.RecoveryQuestDetails(
            targetColdBaths = 4
        )


    ),
    Quests(
        id = "quest02",
        title = "Vital Flow: Essence Restoration",
        description = "Face the Purification of Immovable Ice. This challenge shall forge your will and temper your spirit. With each immersion, the Frigid Baptism of Temperance purifies you, strenghens your Elemental Defense and clears your mind for crucial decisions.",
        types = listOf(Improvement.RECOVERY),
        status = QuestStatus.NOT_STARTED,
        date = Date(),
        rewards = QuestRewards(
            xp = 10,
            coins = 2,
            attributes = QuestAttributes(
                health = 10
            ),
        ),
        streak = QuestStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStreakDate = null
        ),
        details = QuestDetails.RecoveryQuestDetails(
            targetWater = 70
        )
    ),
    Quests(
        id = "quest03",
        title = "Dominion of Gravity: Unyielding Grip",
        description = "Suspend your body against the relentless force of gravity to develop an Unyielding Grip and strengthen your Dimensional Endurance. This training prepares you to climb any obstacle and bear the weight of battle.",
        types = listOf(Improvement.STRENGTH),
        status = QuestStatus.NOT_STARTED,
        date = Date(),
        rewards = QuestRewards(
            xp = 10,
            coins = 2,
            attributes = QuestAttributes(
                strength = 5,
                health = 5
            ),
        ),
        streak = QuestStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStreakDate = null
        ),
        details = QuestDetails.StrengthQuestDetails(
            progressionIncrement = 60f,
            targetTime = 60
        )
    ),
    Quests(
        id = "quest04",
        title = "Forging the Demon's Back",
        description = "This Reclined Warrior Technique perfects your back and bicep coordination. With each repetition, you forge the Hunter's Back, improving your ability to manipulate your own body and master Body Mastery: Earthly Attraction.",
        types = listOf(Improvement.STRENGTH),
        status = QuestStatus.NOT_STARTED,
        date = Date(),
        rewards = QuestRewards(
            xp = 10,
            coins = 2,
            attributes = QuestAttributes(
                strength = 5,
                health = 5
            ),
        ),
        streak = QuestStreak(
            currentStreak = 0,
            longestStreak = 0,
            lastStreakDate = null
        ),
        details = QuestDetails.StrengthQuestDetails(
            progressionIncrement = 5f,
            targetReps = 10
        )
    ),
)*/