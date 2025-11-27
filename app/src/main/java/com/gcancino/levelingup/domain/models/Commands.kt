package com.gcancino.levelingup.domain.models

sealed class Commands {
    object Start : Commands()
    object Stop : Commands()
    object Pause : Commands()
    object Resume : Commands()
    object Save : Commands()
}