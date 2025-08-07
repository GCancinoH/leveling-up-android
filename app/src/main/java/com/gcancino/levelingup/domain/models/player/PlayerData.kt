package com.gcancino.levelingup.domain.models.player

import com.gcancino.levelingup.domain.models.Player

data class PlayerData(
    val player: Player? = null,
    val attributes: Attributes? = null,
    val progress: Progress? = null,
    val streak: Streak? = null
)
