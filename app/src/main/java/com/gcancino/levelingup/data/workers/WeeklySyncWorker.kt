package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.data.local.database.dao.IdentityProfileDao
import com.gcancino.levelingup.data.local.database.dao.WeeklyReportDao
import com.gcancino.levelingup.data.local.database.entities.WeeklyReportEntity
import com.gcancino.levelingup.data.network.IdentityApiService
import com.gcancino.levelingup.data.network.dto.*
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.Json
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.inject.Inject

@HiltWorker
class WeeklySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val identityProfileDao: IdentityProfileDao,
    private val standardEntryDao: DailyStandardEntryDao,
    private val weeklyReportDao: WeeklyReportDao,
    private val generatedQuestManager: GeneratedQuestManager,
    private val apiService: IdentityApiService
) : CoroutineWorker(context, params) {

    private val TAG  = "WeeklySyncWorker"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("WeeklySyncWorker started")

        val uid = auth.currentUser?.uid ?: run {
            Timber.tag(TAG).w("No authenticated user")
            return@withContext Result.success()
        }

        return@withContext try {
            generateWeeklyReport(uid)
            Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Weekly report failed")
            Result.retry()
        }
    }

    private suspend fun generateWeeklyReport(uid: String) {
        val today     = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd   = weekStart.plusDays(7)
        val startMs   = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs     = weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Idempotent — skip if report exists
        if (weeklyReportDao.getForWeek(uid, startMs, endMs) != null) {
            Timber.tag(TAG).d("Report already exists for this week")
            return
        }

        val profileEntity = identityProfileDao.get(uid) ?: return
        val roles: List<Role> = json.decodeFromString(profileEntity.roles)
        val standards: List<IdentityStandard> = json.decodeFromString(profileEntity.standards)

        val entries      = standardEntryDao.getForDay(uid, startMs, endMs)
        val entriesByDay = entries.groupBy {
            it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

        val requestDto = WeeklyReportRequestDto(
            uid = uid,
            identityStatement = profileEntity.identityStatement,
            roles = roles.map { RoleRequestDto(it.id, it.name) },
            standards = standards.map { StandardRequestDto(it.id, it.title, it.type.name, it.roleId) },
            weekEntries = entriesByDay.map { (date, dayEntries) ->
                WeekEntryRequestDto(
                    date = date,
                    standardsCompleted = dayEntries.filter { it.isCompleted }.map { it.standardId },
                    standardsTotal = dayEntries.map { it.standardId }
                )
            }
        )

        val responseDto = apiService.generateWeeklyReport(requestDto)

        val reportId = UUID.randomUUID().toString()
        weeklyReportDao.insert(
            WeeklyReportEntity(
                id                = reportId,
                uID               = uid,
                weekStart         = Date(startMs),
                overallScore      = responseDto.overallScore,
                headline          = responseDto.headline,
                strongestRole     = responseDto.strongestRole,
                weakestRole       = responseDto.weakestRole,
                patternIdentified = responseDto.patternIdentified,
                mirrorInsight     = responseDto.mirrorInsight,
                oneCorrection     = responseDto.oneCorrection,
                identityAlignment = responseDto.identityAlignment,
                generatedAt       = Date(),
                isSynced          = false
            )
        )
        Timber.tag(TAG).i("✔ Weekly report saved")

        responseDto.generatedQuest?.let { questJson ->
            generatedQuestManager.saveQuestFromReport(uid, reportId, questJson)
        }
    }
}