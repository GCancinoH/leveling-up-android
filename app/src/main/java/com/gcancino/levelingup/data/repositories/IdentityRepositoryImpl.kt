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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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
    private val firestore: FirebaseFirestore
) : IdentityRepository {

    private val TAG  = "IdentityRepository"
    private val gson = Gson()

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

    // Type helpers
    private val rolesType = object : TypeToken<List<Role>>() {}.type
    private val standardsType = object : TypeToken<List<IdentityStandard>>() {}.type

    // Mappers
    private fun IdentityProfileEntity.toDomain() = IdentityProfile(
        id = id,
        uID = uID,
        identityStatement = identityStatement,
        roles = gson.fromJson(roles, rolesType),
        standards = gson.fromJson(standards, standardsType),
        createdAt = createdAt,
        isSynced = isSynced
    )

    private fun IdentityProfile.toEntity() = IdentityProfileEntity(
        id = id,
        uID = uID,
        identityStatement = identityStatement,
        roles = gson.toJson(roles),
        standards = gson.toJson(standards),
        createdAt = createdAt,
        isSynced = isSynced
    )

    private fun DailyStandardEntryEntity.toDomain() = DailyStandardEntry(
        id = id,
        uID = uID,
        standardId = standardId,
        standardTitle = standardTitle,
        standardType = StandardType.valueOf(standardType),
        roleId = roleId,
        roleName = roleName,
        date = date,
        isCompleted = isCompleted,
        completedAt = completedAt,
        xpAwarded = xpAwarded,
        autoValidated = autoValidated,
        penaltyApplied = penaltyApplied,
        isSynced = isSynced
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
            Resource.Error(e.message ?: "Error guardando perfil de identidad")
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

            val roles: List<Role> = gson.fromJson(profile.roles, rolesType)
            val standards: List<IdentityStandard> = gson.fromJson(profile.standards, standardsType)

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
            Resource.Error(e.message ?: "Error generando entradas")
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
                gson.fromJson<List<Role>>(it.roles, rolesType)
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

            awardXPToPlayer(uID, entry.xpAwarded)

            Timber.tag(TAG).d(
                "Estándar completado → '${entry.standardTitle}' " +
                        "[${entry.roleName}] | +${entry.xpAwarded} XP"
            )

            Resource.Success(entry.xpAwarded)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "completeStandard() falló")
            Resource.Error(e.message ?: "Error completando estándar")
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
                gson.fromJson(it.standards, standardsType)
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
            awardXPToPlayer(uID, trainingXP)

            Timber.tag(TAG).i("✔ Estándar(es) TRAINING auto-validado(s) → +$trainingXP XP")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "autoValidateTraining() falló")
            Resource.Error(e.message ?: "Error en auto-validación")
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

            // Deducir XP
            val progress = playerProgressDao.getPlayerProgress(uID)
            if (progress != null) {
                val newXP    = maxOf(0, (progress.exp ?: 0) - totalXPLost)
                val newLevel = LevelCalculator.calculateLevel(newXP)
                playerProgressDao.updatePlayerProgress(
                    progress.copy(exp = newXP, level = newLevel)
                )
            }

            // Resetear streak
            val streak = playerStreakDao.getPlayerStreak(uID)
            val streakLost = streak?.currentStreak ?: 0
            if (streak != null) playerStreakDao.updateStreak(uID, 0, Date())

            // Marcar como penalizados
            incomplete.forEach { standardEntryDao.markPenaltyApplied(it.id) }

            Timber.tag(TAG).i(
                "%snull", "✔ Penalización aplicada → " +
                        "${incomplete.size} estándar(es) incumplido(s) | "
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "applyMidnightPenalty() falló")
            Resource.Error(e.message ?: "Error aplicando penalización")
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
            Resource.Error(e.message ?: "Sync fallido")
        }
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────────

    private suspend fun awardXPToPlayer(uID: String, xp: Int) {
        val progress = playerProgressDao.getPlayerProgress(uID) ?: return
        val newXP    = (progress.exp ?: 0) + xp
        val newLevel = LevelCalculator.calculateLevel(newXP)
        playerProgressDao.updatePlayerProgress(progress.copy(exp = newXP, level = newLevel))
        Timber.tag(TAG).d("XP otorgado → +$xp | total: $newXP | nivel: $newLevel")
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