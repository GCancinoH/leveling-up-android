package com.gcancino.levelingup.domain.models.identity

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
            label = "Sin estándares",
            color = IdentityScoreColor.NEUTRAL
        )

        fun calculate(
            entries: List<DailyStandardEntry>,
            roles: List<Role>
        ): IdentityScore {
            if (entries.isEmpty()) return EMPTY

            // Score global
            val total     = entries.size
            val completed = entries.count { it.isCompleted }
            val overall   = completed.toFloat() / total.toFloat()

            // Score por rol — calculado desde las entradas de-normalizadas
            val byRole = roles.associate { role ->
                val roleEntries    = entries.filter { it.roleId == role.id }
                val roleCompleted  = roleEntries.count { it.isCompleted }
                val roleTotal      = roleEntries.size
                role.id to RoleScore(
                    roleId     = role.id,
                    roleName   = role.name,
                    roleColor  = role.color,
                    percentage = if (roleTotal > 0) roleCompleted.toFloat() / roleTotal else 0f,
                    completed  = roleCompleted,
                    total      = roleTotal
                )
            }

            val (label, color) = when {
                overall >= 1.0f -> "Identidad cumplida 🔥"  to IdentityScoreColor.PERFECT
                overall >= 0.8f -> "Casi perfecto"           to IdentityScoreColor.HIGH
                overall >= 0.5f -> "Parcialmente alineado"   to IdentityScoreColor.MEDIUM
                overall > 0f    -> "Debajo de tu estándar"   to IdentityScoreColor.LOW
                else            -> "Sin progreso hoy"        to IdentityScoreColor.NONE
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