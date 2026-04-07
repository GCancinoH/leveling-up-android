package com.gcancino.levelingup.domain.models.identity

import java.util.Date

data class WeeklyReport(
    val id: String,
    val uID: String,
    val weekStart: Date,              // lunes de esa semana
    val overallScore: Float,          // 0.0 - 1.0
    val headline: String,             // frase poderosa del LLM
    val strongestRole: String,
    val weakestRole: String,
    val patternIdentified: String,
    val mirrorInsight: String,        // síntesis de Mirror Mode
    val oneCorrection: String,        // la corrección más importante
    val identityAlignment: String,    // honest assessment
    val generatedAt: Date,
    val isSynced: Boolean = false
)