package com.gcancino.levelingup.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyReportRequestDto(
    val uid: String,
    @SerialName("identity_statement") val identityStatement: String,
    val roles: List<RoleRequestDto>,
    val standards: List<StandardRequestDto>,
    @SerialName("week_entries") val weekEntries: List<WeekEntryRequestDto>
)

@Serializable
data class RoleRequestDto(
    val id: String,
    val name: String
)

@Serializable
data class StandardRequestDto(
    val id: String,
    val title: String,
    val type: String,
    @SerialName("role_id") val roleId: String
)

@Serializable
data class WeekEntryRequestDto(
    val date: String,
    @SerialName("standards_completed") val standardsCompleted: List<String>,
    @SerialName("standards_total") val standardsTotal: List<String>,
    @SerialName("evening_answers") val eveningAnswers: List<String> = emptyList()
)
