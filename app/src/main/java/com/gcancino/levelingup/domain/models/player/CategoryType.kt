package com.gcancino.levelingup.domain.models.player

enum class CategoryType {
    CATEGORY_BEGINNER,
    CATEGORY_INTERMEDIATE,
    CATEGORY_ADVANCED,
    UNKNOWN;

    companion object {
        fun fromString(value: String?) : CategoryType {
            return value?.let {
                try {
                    valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    UNKNOWN
                }
            } ?: UNKNOWN
        }
    }
}