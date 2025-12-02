package com.gcancino.levelingup.domain.usecase

import com.gcancino.levelingup.domain.models.Quests
import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestType
import javax.inject.Inject

class UpdateQuestProgressUseCase @Inject constructor() {

    operator fun invoke(quest: Quests, achievedValue: Int): Quests {
        if (quest.details?.type != QuestType.STRENGTH) {
            return quest
        }

        val updatedDetails = getUpdatedDetails(quest.details, achievedValue)
        val updatedRewards = getUpdatedRewards(quest.rewards, updatedDetails)

        return quest.copy(
            details = updatedDetails,
            rewards = updatedRewards
        )
    }

    private fun getUpdatedDetails(details: QuestDetails?, achievedValue: Int): QuestDetails {
        val currentDetails = details ?: QuestDetails()
        var currentTarget = (currentDetails.targetReps ?: currentDetails.targetTime) ?: 0
        val defaultValue = currentDetails.defaultTarget ?: 0
        val maxValue = currentDetails.maxTarget ?: (defaultValue * 2)
        val increment = currentDetails.progressionIncrement?.toInt() ?: 1

        if (currentDetails.isFirstAttempt == true) {
            currentTarget = if (achievedValue > defaultValue) achievedValue else defaultValue
        } else if (achievedValue >= currentTarget) {
            currentTarget += increment
        }

        if (currentTarget > maxValue) {
            currentTarget = maxValue
        }

        return currentDetails.copy(
            targetReps = if (currentDetails.targetReps != null) currentTarget else null,
            targetTime = if (currentDetails.targetTime != null) currentTarget else null,
            isFirstAttempt = false
        )
    }

    private fun getUpdatedRewards(rewards: QuestRewards?, updatedDetails: QuestDetails): QuestRewards {
        val currentRewards = rewards ?: QuestRewards()
        val currentTarget = (updatedDetails.targetReps ?: updatedDetails.targetTime) ?: 0
        val defaultValue = updatedDetails.defaultTarget ?: 0
        val maxValue = updatedDetails.maxTarget ?: (defaultValue * 2)
        val baseCoins = currentRewards.baseCoins ?: 0
        val baseXp = currentRewards.baseXp ?: 0

        val progress = (currentTarget - defaultValue).toFloat() / (maxValue - defaultValue).toFloat()
        val scaledCoins = baseCoins + (baseCoins * progress).toInt()
        val scaledXp = baseXp + (baseXp * progress).toInt()

        return currentRewards.copy(
            coins = scaledCoins,
            xp = scaledXp
        )
    }
}
