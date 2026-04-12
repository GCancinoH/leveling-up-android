package com.gcancino.levelingup.domain.models.identity

import com.gcancino.levelingup.R

data class RoleScore(
    val roleId: String,
    val roleName: String,
    val roleColor: String,
    val percentage: Float,
    val completed: Int,
    val total: Int
)

data class IdentityScore(
    val overall: Float,
    val completedStandards: Int,
    val totalStandards: Int,
    val byRole: Map<String, RoleScore>,  // roleId → RoleScore
    val label: String,
    val color: IdentityScoreColor
) {
    companion object {

        val EMPTY = IdentityScore(
            overall = 0f,
            completedStandards = 0,
            totalStandards = 0,
            byRole = emptyMap(),
            label = R.string.identity_score_empty.toString(),
            color = IdentityScoreColor.NEUTRAL
        )

        fun calculate(
            entries: List<DailyStandardEntry>,
            roles: List<Role>
        ): IdentityScore {
            if (entries.isEmpty()) return EMPTY

            val total     = entries.size
            val completed = entries.count { it.isCompleted }
            val failed    = entries.count { it.isFailed }  // ← NUEVO: fallos activos

            // isFailed pesa más que simplemente no completar:
            // un fallo activo resta 1.5x en el score
            val effectiveCompleted = completed.toFloat() - (failed * 0.5f)
            val overall = (effectiveCompleted / total.toFloat()).coerceIn(0f, 1f)

            val byRole = roles.associate { role ->
                val roleEntries   = entries.filter { it.roleId == role.id }
                val roleCompleted = roleEntries.count { it.isCompleted }
                val roleFailed    = roleEntries.count { it.isFailed }
                val roleTotal     = roleEntries.size
                val roleScore     = if (roleTotal > 0)
                    ((roleCompleted - roleFailed * 0.5f) / roleTotal).coerceIn(0f, 1f)
                else 0f

                role.id to RoleScore(
                    roleId     = role.id,
                    roleName   = role.name,
                    roleColor  = role.color,
                    percentage = roleScore,
                    completed  = roleCompleted,
                    total      = roleTotal
                )
            }

            val (label, color) = when {
                overall >= 1.0f -> R.string.identity_score_lbl_perfect.toString() to IdentityScoreColor.PERFECT
                overall >= 0.8f -> R.string.identity_score_lbl_high.toString() to IdentityScoreColor.HIGH
                overall >= 0.5f -> R.string.identity_score_lbl_medium.toString() to IdentityScoreColor.MEDIUM
                overall > 0f    -> R.string.identity_score_lbl_low.toString() to IdentityScoreColor.LOW
                else -> R.string.identity_score_lbl_none.toString() to IdentityScoreColor.NONE
            }

            return IdentityScore(
                overall            = overall,
                completedStandards = completed,
                totalStandards     = total,
                byRole             = byRole,
                label              = label,
                color              = color
            )
        }
    }
}

enum class IdentityScoreColor { PERFECT, HIGH, MEDIUM, LOW, NONE, NEUTRAL }