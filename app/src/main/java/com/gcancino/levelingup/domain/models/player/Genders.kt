package com.gcancino.levelingup.domain.models.player

enum class Genders {
    FEMALE,
    MALE;

    companion object {
        fun fromString(value: String?): Genders? {
            return when (value?.uppercase()) {
                "FEMALE" -> FEMALE
                "MALE" -> MALE
                else -> null
            }
        }
    }
}