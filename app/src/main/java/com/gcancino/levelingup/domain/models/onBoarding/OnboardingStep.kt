package com.gcancino.levelingup.domain.models.onBoarding

enum class OnboardingStep(val index: Int) {
    WELCOME(0),
    PERSONAL_INFO(1),
    PHYSICAL_AND_COMPOSITION(2),
    BODY_MEASUREMENT(3),
    IMPROVEMENTS(4),
    PHOTOS(5);

    companion object {
        fun fromIndex(index: Int) = entries.firstOrNull { it.index == index } ?: WELCOME
        val totalSteps = entries.size
    }
}