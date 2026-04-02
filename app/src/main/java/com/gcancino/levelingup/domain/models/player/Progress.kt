package com.gcancino.levelingup.domain.models.player

import java.util.Date

data class Progress(
    val uid: String,
    val coins: Int? = null,
    val exp: Int? = null,
    val level: Int? = null,
    val availablePoints: Int = 0,
    val currentCategory: CategoryType = CategoryType.CATEGORY_BEGINNER,
    val lastLevelUpdate: Date? = null,
    val lastCategoryUpdate: Date? = null
)
