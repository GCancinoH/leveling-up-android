package com.gcancino.levelingup.domain.models

import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import java.util.Date

data class Player(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoURL: String? = null,
    val birthDate: Date? = null,
    val age: Int? = null,
    val height: Double? = null,
    val gender: Genders? = null,
    val improvements: List<Improvement>? = null
)
