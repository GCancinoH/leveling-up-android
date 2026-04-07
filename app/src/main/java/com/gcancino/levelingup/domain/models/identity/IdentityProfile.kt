package com.gcancino.levelingup.domain.models.identity

import kotlinx.serialization.Serializable
import java.util.Date
import com.gcancino.levelingup.core.DateSerializer

@Serializable
data class IdentityProfile(
    val id: String = "",
    val uID: String = "",
    val identityStatement: String = "",
    val roles: List<Role> = emptyList(),
    val standards: List<IdentityStandard> = emptyList(),
    val createdAt: @Serializable(with = DateSerializer::class) Date = Date(),
    val isSynced: Boolean = false
) {
    // Estándares de un rol específico
    fun standardsForRole(roleId: String) = standards.filter { it.roleId == roleId }

    // Roles con al menos un estándar activo
    fun activeRoles() = roles.filter { role ->
        standards.any { it.roleId == role.id && it.isActive }
    }
}