package com.gcancino.levelingup.domain.models

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
            id       = "m_anchor_1",
            text     = "What are three things you are genuinely grateful for today?",
            hint     = "Be specific — 'my morning coffee' beats 'my health'",
            isAnchor = true
        ),
        Question(
            id       = "m_anchor_2",
            text     = "What would make today a great day?",
            hint     = "Think in terms of actions, not outcomes",
            isAnchor = true
        ),
        Question(
            id       = "m_anchor_3",
            text     = "What is the single most important thing you must do today?",
            hint     = "If you could only do one thing, what moves the needle most?",
            isAnchor = true
        )
    )

    // Values Alignment
    val MORNING_ROTATING = listOf(
        Question("m_r_1",  "What kind of person do you want to be today? Name 3 qualities."),
        Question("m_r_2",  "What fear or resistance might come up today, and how will you respond?"),
        Question("m_r_3",  "Who in your life deserves more of your attention today?"),
        Question("m_r_4",  "What is one thing you can do today that your future self will thank you for?"),
        Question("m_r_5",  "If today were the last day of the year, what would matter most?"),
        Question("m_r_6",  "What energy do you want to bring into your interactions today?"),
        Question("m_r_7",  "What would the best version of you do today that the average version would avoid?"),
        Question("m_r_8",  "What limiting belief might hold you back today, and what is the counter-truth?"),
        Question("m_r_9",  "How can you be 1% better than yesterday in one specific area?"),
        Question("m_r_10", "What does your body need from you today?"),
        Question("m_r_11", "What relationship in your life needs intentional investment this week?"),
        Question("m_r_12", "What is one thing you are avoiding that, if done, would create the most relief?"),
        Question("m_r_13", "What would it look like to fully commit to your purpose today?"),
        Question("m_r_14", "What assumption about today might be limiting you before it even starts?"),
        Question("m_r_15", "If you could send a message to yourself tonight, what would you want it to say?")
    )

    /**
     * Evening Anchor
     */

    // CBT-proven: naming wins + struggles activates prefrontal cortex, reduces amygdala reactivity
    val EVENING_ANCHOR = listOf(
        Question(
            id       = "e_anchor_1",
            text     = "What were your biggest wins today — big or small?",
            hint     = "Rewiring requires you to consciously notice progress",
            isAnchor = true
        ),
        Question(
            id       = "e_anchor_2",
            text     = "What was the hardest moment today, and what did it teach you?",
            hint     = "Reframing struggle as data is a core CBT technique",
            isAnchor = true
        ),
        Question(
            id       = "e_anchor_3",
            text     = "On a scale of 1–10, how aligned was today with your values? What would have made it a 10?",
            hint     = "Naming the gap between actual and ideal creates growth intention",
            isAnchor = true
        )
    )

    // CBT-style thought examination + deep introspection for neural rewiring

    val EVENING_ROTATING = listOf(
        Question("e_r_1",  "What thought pattern showed up repeatedly today? Is it serving you?"),
        Question("e_r_2",  "Where did you give your energy today — was it worth it?"),
        Question("e_r_3",  "What emotion did you avoid feeling today, and what was underneath it?"),
        Question("e_r_4",  "What did you say yes to that you should have said no to?"),
        Question("e_r_5",  "What story did you tell yourself today that might not be true?"),
        Question("e_r_6",  "Who did you show up for today, and how well did you do it?"),
        Question("e_r_7",  "What decision today are you least at peace with? What would you change?"),
        Question("e_r_8",  "What did your body tell you today that your mind ignored?"),
        Question("e_r_9",  "Where did you operate from fear rather than intention?"),
        Question("e_r_10", "What habit either served or sabotaged you today?"),
        Question("e_r_11", "What would someone who deeply respects you say about how you spent today?"),
        Question("e_r_12", "What unfinished thought or feeling has been following you all day?"),
        Question("e_r_13", "What did you learn today that you want to remember in 5 years?"),
        Question("e_r_14", "Where did you shrink today when you could have stepped up?"),
        Question("e_r_15", "What conversation did you have (or avoid) that needs to happen?"),
        Question("e_r_16", "What are you pretending not to know about a situation in your life?"),
        Question("e_r_17", "How did you treat yourself today — would you treat a friend that way?"),
        Question("e_r_18", "What pattern are you ready to break, and what would replacing it look like?"),
        Question("e_r_19", "What are you tolerating in your life that is quietly draining your energy?"),
        Question("e_r_20", "If today was a chapter in your life story, what would its title be?")
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
