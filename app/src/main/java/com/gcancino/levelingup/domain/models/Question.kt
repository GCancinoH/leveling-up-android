package com.gcancino.levelingup.domain.models

import androidx.annotation.StringRes

/*data class Question(
    val id: String,
    val text: String,
    val hint: String = "",
    val isAnchor: Boolean = false
)*/
data class Question(
    val id: String,
    @get:StringRes val textRes: Int,
    @get:StringRes val hint: Int? = null,
    val isAnchor: Boolean = false
)