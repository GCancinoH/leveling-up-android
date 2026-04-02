package com.gcancino.levelingup.domain.models.bodyComposition

fun Double.kgToLbs(): Double = this * 2.20462
fun Double.lbsToKg(): Double = this / 2.20462
fun Double.cmToInches(): Double = this / 2.54
fun Double.inchesToCm(): Double = this * 2.54