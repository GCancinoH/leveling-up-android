package com.gcancino.levelingup.domain.models.player

import android.telephony.SignalStrength

data class Attributes(
    val uid: String,
    val strength: Int? = null,
    val endurance: Int? = null,
    val intelligence: Int? = null,
    val mobility: Int? = null,
    val health: Int? = null,
    val finance: Int? = null,
)
