package com.gcancino.levelingup.domain.models

import com.gcancino.levelingup.R
import java.time.LocalDate

/**
 * QuestionBank
 *
 * Designed around neuroscience research on neuroplasticity and journaling:
 * - Morning: gratitude (serotonin/dopamine), values alignment, best-self visualization
 * - Evening: CBT-style thought examination, wins/growth reflection, anchor questions
 *
 * Structure:
 * - ANCHOR questions: same every day (builds neural pathways through repetition)
 * - ROTATING questions: randomly selected from pool (novelty drives neuroplasticity)
 */
object QuestionBank {
    /**
     * Morning Anchor
     */

    // Gratitude
    val MORNING_ANCHOR = listOf(
        Question(
            id = "m_anchor_1",
            textRes = R.string.qb_morningAnchor_1,
            hint = R.string.qb_morningAnchor_1_hint,
            isAnchor = true
        ),
        Question(
            id = "m_anchor_2",
            textRes = R.string.qb_morningAnchor_2,
            hint = R.string.qb_morningAnchor_2_hint,
            isAnchor = true
        ),
        Question(
            id = "m_anchor_3",
            textRes = R.string.qb_morningAnchor_3,
            hint = R.string.qb_morningAnchor_3_hint,
            isAnchor = true
        )
    )

    // Values Alignment
    val MORNING_ROTATING = listOf(
        Question("m_r_1", R.string.qb_morningRotating_1),
        Question("m_r_2", R.string.qb_morningRotating_2),
        Question("m_r_3", R.string.qb_morningRotating_3),
        Question("m_r_4", R.string.qb_morningRotating_4),
        Question("m_r_5", R.string.qb_morningRotating_5),
        Question("m_r_6", R.string.qb_morningRotating_6),
        Question("m_r_7", R.string.qb_morningRotating_7),
        Question("m_r_8", R.string.qb_morningRotating_8),
        Question("m_r_9", R.string.qb_morningRotating_9),
        Question("m_r_10", R.string.qb_morningRotating_10),
        Question("m_r_11", R.string.qb_morningRotating_11),
        Question("m_r_12", R.string.qb_morningRotating_12),
        Question("m_r_13", R.string.qb_morningRotating_13),
        Question("m_r_14", R.string.qb_morningRotating_14),
        Question("m_r_15", R.string.qb_morningRotating_15)
    )

    /**
     * Evening Anchor
     */
    val EVENING_ANCHOR = listOf(
        Question(
            id = "e_anchor_1",
            textRes =  R.string.qb_eveningAnchor_1,
            hint = R.string.qb_eveningAnchor_hint_1,
            isAnchor = true
        ),
        Question(
            id = "e_anchor_2",
            textRes =  R.string.qb_eveningAnchor_2,
            hint = R.string.qb_eveningAnchor_hint_2,
            isAnchor = true
        ),
        Question(
            id = "e_anchor_3",
            textRes =  R.string.qb_eveningAnchor_3,
            hint = R.string.qb_eveningAnchor_hint_3,
            isAnchor = true
        ),
        Question(
            id       = "e_mirror",
            textRes  = R.string.qb_mirrorMode,
            isAnchor = true
        )

    )

    val EVENING_ROTATING = listOf(
        Question("e_r_1",  R.string.qb_eveningRotating_1),
        Question("e_r_2",  R.string.qb_eveningRotating_2),
        Question("e_r_3",  R.string.qb_eveningRotating_3),
        Question("e_r_4",  R.string.qb_eveningRotating_4),
        Question("e_r_5",  R.string.qb_eveningRotating_5),
        Question("e_r_6",  R.string.qb_eveningRotating_6),
        Question("e_r_7",  R.string.qb_eveningRotating_7),
        Question("e_r_8",  R.string.qb_eveningRotating_8),
        Question("e_r_9",  R.string.qb_eveningRotating_9),
        Question("e_r_10", R.string.qb_eveningRotating_10),
        Question("e_r_11", R.string.qb_eveningRotating_11),
        Question("e_r_12", R.string.qb_eveningRotating_12),
        Question("e_r_13", R.string.qb_eveningRotating_13),
        Question("e_r_14", R.string.qb_eveningRotating_14),
        Question("e_r_15", R.string.qb_eveningRotating_15),
        Question("e_r_16", R.string.qb_eveningRotating_16),
        Question("e_r_17", R.string.qb_eveningRotating_17),
        Question("e_r_18", R.string.qb_eveningRotating_18),
        Question("e_r_19", R.string.qb_eveningRotating_19),
        Question("e_r_20", R.string.qb_eveningRotating_20)
    )

    // Daily selection model

    /**
     * Selects 2 rotating morning questions seeded by today's date.
     * Same date always returns the same questions (deterministic) so the
     * user sees consistent questions throughout the day.
     */
    fun getTodaysMorningRotating(): List<Question> {
        val seed = LocalDate.now().toEpochDay()
        return MORNING_ROTATING.shuffled(java.util.Random(seed)).take(2)
    }

    fun getTodaysEveningRotating(): List<Question> {
        val seed = LocalDate.now().toEpochDay() + 1000L // offset to differ from morning
        return EVENING_ROTATING.shuffled(java.util.Random(seed)).take(2)
    }

    fun getTodaysMorningQuestions(): List<Question> =
        MORNING_ANCHOR + getTodaysMorningRotating()

    fun getTodaysEveningQuestions(): List<Question> =
        EVENING_ANCHOR + getTodaysEveningRotating()
}
