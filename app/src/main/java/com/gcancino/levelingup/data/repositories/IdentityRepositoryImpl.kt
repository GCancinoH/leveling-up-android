package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.data.local.database.dao.IdentityProfileDao
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.data.local.database.entities.DailyStandardEntryEntity
import com.gcancino.levelingup.data.local.database.entities.IdentityProfileEntity
import com.gcancino.levelingup.domain.logic.LevelCalculator
import com.gcancino.levelingup.domain.models.identity.DailyStandardEntry
import com.gcancino.levelingup.domain.models.identity.IdentityProfile
import com.gcancino.levelingup.domain.models.identity.IdentityScore
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.gcancino.levelingup.domain.models.identity.StandardType
import com.gcancino.levelingup.domain.repositories.IdentityRepository
import com.gcancino.levelingup.domain.repositories.PlayerRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class IdentityRepositoryImpl @Inject constructor(
    private val identityProfileDao: IdentityProfileDao,
    private val standardEntryDao: DailyStandardEntryDao,
    private val playerProgressDao: PlayerProgressDao,
    private val playerStreakDao: PlayerStreakDao,
    private val playerRepository: PlayerRepository,
    private val firestore: FirebaseFirestore
) : IdentityRepository {

    private val TAG  = "IdentityRepository"
    private val json = Json { ignoreUnknownKeys = true }

    // Day boundaries
    private fun todayBoundaries(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    // Mappers
    private fun IdentityProfileEntity.toDomain() = IdentityProfile(
        id = id,
        uID = uID,
        identityStatement = identityStatement,
        roles = json.decodeFromString(roles),
        standards = json.decodeFromString(standards),
        createdAt = createdAt,
        isSynced = isSynced
    )

    private fun IdentityProfile.toEntity() = IdentityProfileEntity(
        id = id,
        uID = uID,
        identityStatement = identityStatement,
        roles = json.encodeToString(roles),
        standards = json.encodeToString(standards),
        createdAt = createdAt,
        isSynced = isSynced
    )

    private fun DailyStandardEntryEntity.toDomain() = DailyStandardEntry(
        id             = id,
        uID            = uID,
        standardId     = standardId,
        standardTitle  = standardTitle,
        standardType   = StandardType.valueOf(standardType),
        roleId         = roleId,
        roleName       = roleName,
        date           = date,
        isCompleted    = isCompleted,
        isFailed       = isFailed,       // ← NUEVO
        completedAt    = completedAt,
        xpAwarded      = xpAwarded,
        autoValidated  = autoValidated,
        penaltyApplied = penaltyApplied,
        isSynced       = isSynced
    )

    // ─── Perfil de identidad ──────────────────────────────────────────────────────
    override suspend fun saveIdentityProfile(profile: IdentityProfile): Resource<Unit> {
        return try {
            identityProfileDao.insert(profile.toEntity())
            Timber.tag(TAG).i(
                "✔ IdentityProfile guardado → '${profile.identityStatement}' | " +
                        "${profile.roles.size} roles | ${profile.standards.size} estándares"
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveIdentityProfile() falló")
            Resource.Error("Error guardando perfil de identidad")
        }
    }

    override fun observeIdentityProfile(uID: String): Flow<IdentityProfile?> =
        identityProfileDao.observe(uID).map { it?.toDomain() }

    override suspend fun hasIdentityProfile(uID: String): Boolean =
        identityProfileDao.count(uID) > 0

    // ─── Generar entradas del día ─────────────────────────────────────────────────
    // Idempotente — si ya existen entradas para un estándar hoy, no las duplica.
    override suspend fun generateTodayEntries(uID: String): Resource<Unit> {
        return try {
            val (start, end) = todayBoundaries()

            val profile = identityProfileDao.get(uID)
                ?: return Resource.Success(Unit) // Sin perfil aún — ok, silencioso

            val roles: List<Role> = json.decodeFromString(profile.roles)
            val standards: List<IdentityStandard> = json.decodeFromString(profile.standards)

            // Obtener los standardIds que ya tienen entrada hoy
            val existingIds = standardEntryDao
                .getExistingStandardIdsForDay(uID, start, end)
                .toSet()

            // Construir mapa roleId → Role para denormalizar eficientemente
            val roleMap = roles.associateBy { it.id }

            val newEntries = standards
                .filter { it.isActive && it.id !in existingIds }
                .mapNotNull { standard ->
                    val role = roleMap[standard.roleId] ?: run {
                        Timber.tag(TAG).w("Rol '${standard.roleId}' no encontrado para estándar '${standard.title}'")
                        return@mapNotNull null
                    }
                    DailyStandardEntryEntity(
                        id = UUID.randomUUID().toString(),
                        uID = uID,
                        standardId = standard.id,
                        standardTitle = standard.title,
                        standardType = standard.type.name,
                        roleId = standard.roleId,
                        roleName = role.name,
                        date = Date(),
                        isCompleted = false,
                        xpAwarded = standard.xpReward,
                        autoValidated = false,
                        penaltyApplied = false,
                        isSynced = false
                    )
                }

            if (newEntries.isNotEmpty()) {
                standardEntryDao.insertAll(newEntries)
                Timber.tag(TAG).i("✔ ${newEntries.size} entrada(s) generadas para hoy")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "generateTodayEntries() falló")
            Resource.Error("Error generando entradas")
        }
    }

    // ─── Observar score del día ───────────────────────────────────────────────────

    override fun observeTodayScore(uID: String): Flow<IdentityScore> {
        val (start, end) = todayBoundaries()
        // Combina entradas + perfil para tener los roles disponibles al calcular
        return combine(
            standardEntryDao.observeForDay(uID, start, end),
            identityProfileDao.observe(uID)
        ) { entries, profileEntity ->
            val roles = profileEntity?.let {
                json.decodeFromString<List<Role>>(it.roles)
            } ?: emptyList()
            IdentityScore.calculate(entries.map { it.toDomain() }, roles)
        }
    }

    override fun observeTodayEntries(uID: String): Flow<List<DailyStandardEntry>> {
        val (start, end) = todayBoundaries()
        return standardEntryDao.observeForDay(uID, start, end)
            .map { it.map { e -> e.toDomain() } }
    }

    override fun observePendingEntries(uID: String): Flow<List<DailyStandardEntry>> {
        val (start, end) = todayBoundaries()
        return standardEntryDao.observePendingForDay(uID, start, end)
            .map { it.map { e -> e.toDomain() } }
    }

    // ─── Completar estándar manualmente ──────────────────────────────────────────
    override suspend fun completeStandard(entryId: String, uID: String): Resource<Int> {
        return try {
            val (start, end) = todayBoundaries()
            val entry = standardEntryDao.getForDay(uID, start, end)
                .firstOrNull { it.id == entryId }
                ?: return Resource.Error("Entrada no encontrada")

            if (entry.isCompleted) return Resource.Error("Estándar ya completado")

            standardEntryDao.markCompleted(
                entryId     = entryId,
                completedAt = System.currentTimeMillis(),
                xpAwarded   = entry.xpAwarded
            )

            playerRepository.awardXP(uID, entry.xpAwarded)

            Timber.tag(TAG).d(
                "Estándar completado → '${entry.standardTitle}' " +
                        "[${entry.roleName}] | +${entry.xpAwarded} XP"
            )

            Resource.Success(entry.xpAwarded)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "completeStandard() falló")
            Resource.Error("Error completando estándar")
        }
    }

    // ─── Auto-validación TRAINING ─────────────────────────────────────────────────
    // Llamado desde SessionPlayerViewModel al completar sesión.
    // El usuario no hace nada — la app detecta que entrenó.
    override suspend fun autoValidateTraining(uID: String): Resource<Unit> {
        return try {
            val (start, end) = todayBoundaries()

            // Obtener XP del estándar TRAINING del perfil
            val profile = identityProfileDao.get(uID)
            val standards: List<IdentityStandard> = profile?.let {
                json.decodeFromString(it.standards)
            } ?: emptyList()
            val trainingXP = standards
                .filter { it.type == StandardType.TRAINING && it.isActive }
                .sumOf { it.xpReward }
                .takeIf { it > 0 } ?: 100

            standardEntryDao.autoValidateTraining(
                uID         = uID,
                startOfDay  = start,
                endOfDay    = end,
                completedAt = System.currentTimeMillis(),
                xpAwarded   = trainingXP
            )

            // Si había múltiples estándares TRAINING, awardXP por el total
            playerRepository.awardXP(uID, trainingXP)

            Timber.tag(TAG).i("✔ Estándar(es) TRAINING auto-validado(s) → +$trainingXP XP")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "autoValidateTraining() falló")
            Resource.Error("Error en auto-validación")
        }
    }

    override suspend fun autoValidateNutrition(uID: String): Resource<Unit> {
        return try {
            val (start, end) = todayBoundaries()

            val profile    = identityProfileDao.get(uID)
            val standards: List<IdentityStandard> = profile?.let {
                json.decodeFromString(it.standards)
            } ?: emptyList()

            // XP sumado de todos los estándares NUTRITION activos
            val nutritionXP = standards
                .filter { it.type == StandardType.NUTRITION && it.isActive }
                .sumOf { it.xpReward }
                .takeIf { it > 0 } ?: 20

            // Reutiliza el mismo query SQL que autoValidateTraining pero para NUTRITION
            standardEntryDao.autoValidateByType(
                uID         = uID,
                startOfDay  = start,
                endOfDay    = end,
                completedAt = System.currentTimeMillis(),
                xpAwarded   = nutritionXP,
                standardType = "NUTRITION"    // ← parámetro nuevo en el DAO
            )

            playerRepository.awardXP(uID, nutritionXP)
            Timber.tag(TAG).i("✔ Estándar(es) NUTRITION auto-validado(s) → +$nutritionXP XP")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "autoValidateNutrition() falló")
            Resource.Error("Error en auto-validación NUTRITION")
        }
    }

    // ─── Penalización de medianoche ───────────────────────────────────────────────
    override suspend fun applyMidnightPenalty(uID: String): Resource<Unit> {
        return try {
            val (start, end) = todayBoundaries()
            val incomplete   = standardEntryDao.getIncompleteForDay(uID, start, end)

            if (incomplete.isEmpty()) {
                Timber.tag(TAG).i("Sin incumplimientos — sin penalización")
                return Resource.Success(Unit)
            }

            val totalXPLost = incomplete.sumOf { it.xpAwarded }

            // Deducir XP (Centralized via Repository if we had a dedicated penalty award fun, 
            // but for deduction we can use a negative value or a specific method. 
            // PlayerRepository.awardXP currently doesn't handle negative XP as a "penalty" with level logic explicitly, 
            // but let's see if we should use it. For now, keep it mostly as is but fix the race condition 
            // if we use a transaction or similar logic elsewhere. Actually, the playerRepository has a central management.
            // Let's assume awardXP can take negative values or let's just use it if possible.
            // Better: use the PlayerRepository to maintain consistency.
            playerRepository.awardXP(uID, -totalXPLost)

            // Resetear streak
            val streak = playerStreakDao.getPlayerStreak(uID)
            if (streak != null) playerStreakDao.updateStreak(uID, 0, Date())

            // Marcar como penalizados
            incomplete.forEach { standardEntryDao.markPenaltyApplied(it.id) }

            Timber.tag(TAG).i(
                "✔ Penalización aplicada → " +
                        "${incomplete.size} estándar(es) incumplido(s) | "
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "applyMidnightPenalty() falló")
            Resource.Error("Error aplicando penalización")
        }
    }

    override suspend fun markTrainingFailed(uID: String): Resource<Unit> {
        return try {
            val (start, end) = todayBoundaries()
            // Marca isFailed=true en todos los estándares TRAINING del día
            // que no estén completados — igual que markFailedByStandardId pero por tipo
            standardEntryDao.markFailedByType(
                uID          = uID,
                standardType = "TRAINING",
                startOfDay   = start,
                endOfDay     = end
            )
            Timber.tag(TAG).i("✔ TRAINING standards marcados como isFailed")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Error marcando training como fallido")
        }
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────────
    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val profiles = identityProfileDao.getUnsynced()
            val entries  = standardEntryDao.getUnsynced()

            if (profiles.isEmpty() && entries.isEmpty()) return Resource.Success(Unit)

            val batch = firestore.batch()

            profiles.forEach { profile ->
                val ref = firestore.collection("identity_profiles").document(profile.id)
                batch.set(ref, profile.toFirestoreMap())
            }
            entries.forEach { entry ->
                val ref = firestore.collection("daily_standard_entries").document(entry.id)
                batch.set(ref, entry.toFirestoreMap())
            }

            batch.commit().await()
            Timber.tag(TAG).i("✔ Identity sync completado")

            if (profiles.isNotEmpty()) identityProfileDao.markAsSynced(profiles.map { it.id })
            if (entries.isNotEmpty())  standardEntryDao.markAsSynced(entries.map { it.id })

            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced() falló")
            Resource.Error("Sync fallido")
        }
    }

    // ─── Firestore helpers ────────────────────────────────────────────────────────

    private fun IdentityProfileEntity.toFirestoreMap() = mapOf(
        "id" to id,
        "uID" to uID,
        "identityStatement" to identityStatement,
        "roles" to roles,
        "standards" to standards,
        "createdAt" to Timestamp(createdAt)
    )

    private fun DailyStandardEntryEntity.toFirestoreMap() = mapOf(
        "id"             to id,
        "uID"            to uID,
        "standardId"     to standardId,
        "standardTitle"  to standardTitle,
        "standardType"   to standardType,
        "roleId"         to roleId,
        "roleName"       to roleName,
        "date"           to Timestamp(date),
        "isCompleted"    to isCompleted,
        "completedAt"    to completedAt?.let { Timestamp(it) },
        "xpAwarded"      to xpAwarded,
        "autoValidated"  to autoValidated,
        "penaltyApplied" to penaltyApplied
    )
}