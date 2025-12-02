package com.gcancino.levelingup.domain.usecase

import com.gcancino.levelingup.domain.models.QuestDetails
import com.gcancino.levelingup.domain.models.QuestRewards
import com.gcancino.levelingup.domain.models.QuestType
import com.gcancino.levelingup.domain.models.Quests
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateQuestProgressUseCaseTest {

    private val useCase = UpdateQuestProgressUseCase()

    @Test
    fun `invoke SHOULD return quest unchanged WHEN quest is not STRENGTH type`() {
        // GIVEN
        val quest = Quests(details = QuestDetails(type = QuestType.ENDURANCE))

        // WHEN
        val result = useCase(quest, 10)

        // THEN
        assertEquals(quest, result)
    }

    @Test
    fun `invoke SHOULD update target WHEN first attempt and achieved value is greater than default`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                defaultTarget = 10,
                targetReps = 10,
                isFirstAttempt = true
            )
        )

        // WHEN
        val result = useCase(quest, 15)

        // THEN
        assertEquals(15, result.details?.targetReps)
        assertEquals(false, result.details?.isFirstAttempt)
    }

    @Test
    fun `invoke SHOULD update target with default value WHEN first attempt and achieved value is less than default`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                defaultTarget = 10,
                targetReps = 10,
                isFirstAttempt = true
            )
        )

        // WHEN
        val result = useCase(quest, 5)

        // THEN
        assertEquals(10, result.details?.targetReps)
        assertEquals(false, result.details?.isFirstAttempt)
    }

    @Test
    fun `invoke SHOULD increment target WHEN achieved value is equal to current target`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                targetReps = 10,
                progressionIncrement = 2f
            )
        )

        // WHEN
        val result = useCase(quest, 10)

        // THEN
        assertEquals(12, result.details?.targetReps)
    }

    @Test
    fun `invoke SHOULD increment target WHEN achieved value is greater than current target`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                targetReps = 10,
                progressionIncrement = 2f
            )
        )

        // WHEN
        val result = useCase(quest, 11)

        // THEN
        assertEquals(12, result.details?.targetReps)
    }

    @Test
    fun `invoke SHOULD NOT increment target WHEN achieved value is less than current target`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                targetReps = 10,
                progressionIncrement = 2f
            )
        )

        // WHEN
        val result = useCase(quest, 9)

        // THEN
        assertEquals(10, result.details?.targetReps)
    }

    @Test
    fun `invoke SHOULD cap target at max value`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                targetReps = 118,
                maxTarget = 120,
                progressionIncrement = 5f
            )
        )

        // WHEN
        val result = useCase(quest, 118)

        // THEN
        assertEquals(120, result.details?.targetReps)
    }

    @Test
    fun `invoke SHOULD calculate rewards correctly`() {
        // GIVEN
        val quest = Quests(
            details = QuestDetails(
                type = QuestType.STRENGTH,
                defaultTarget = 10,
                targetReps = 10,
                maxTarget = 100,
                progressionIncrement = 10f
            ),
            rewards = QuestRewards(
                baseCoins = 100,
                baseXp = 1000
            )
        )

        // WHEN
        val result = useCase(quest, 10)

        // THEN
        assertEquals(111, result.rewards?.coins)
        assertEquals(1111, result.rewards?.xp)
    }
}
