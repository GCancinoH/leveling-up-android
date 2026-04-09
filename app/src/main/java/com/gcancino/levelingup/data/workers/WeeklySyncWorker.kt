package com.gcancino.levelingup.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gcancino.levelingup.data.local.database.dao.DailyStandardEntryDao
import com.gcancino.levelingup.data.local.database.dao.IdentityProfileDao
import com.gcancino.levelingup.data.local.database.dao.WeeklyReportDao
import com.gcancino.levelingup.data.local.database.entities.WeeklyReportEntity
import com.gcancino.levelingup.domain.logic.GeneratedQuestManager
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class WeeklySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val identityProfileDao: IdentityProfileDao,
    private val standardEntryDao: DailyStandardEntryDao,
    private val weeklyReportDao: WeeklyReportDao,
    private val generatedQuestManager: GeneratedQuestManager
) : CoroutineWorker(context, params) {

    private val TAG = "WeeklySyncWorker"

    // ← Change to your server URL. For local testing with emulator: http://10.0.2.2:5000
    private val FLASK_BASE_URL = "https://leveling-up-server.vercel.app"

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // LLM can take time
        .build()

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
        val roles: List<Role> = gson.fromJson(profileEntity.roles,
            object : TypeToken<List<Role>>() {}.type)
        val standards: List<IdentityStandard> = gson.fromJson(profileEntity.standards,
            object : TypeToken<List<IdentityStandard>>() {}.type)

        val entries      = standardEntryDao.getForDay(uid, startMs, endMs)
        val entriesByDay = entries.groupBy {
            it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }

        val payload = JSONObject().apply {
            put("uid", uid)
            put("identity_statement", profileEntity.identityStatement)
            put("roles", JSONArray(roles.map { mapOf("id" to it.id, "name" to it.name) }
                .let { gson.toJson(it) }))
            put("standards", JSONArray(standards.map {
                mapOf("id" to it.id, "title" to it.title,
                    "type" to it.type.name, "role_id" to it.roleId)
            }.let { gson.toJson(it) }))

            val weekArray = JSONArray()
            entriesByDay.forEach { (date, dayEntries) ->
                weekArray.put(JSONObject().apply {
                    put("date", date)
                    put("standards_completed",
                        JSONArray(dayEntries.filter { it.isCompleted }.map { it.standardId }))
                    put("standards_total",
                        JSONArray(dayEntries.map { it.standardId }))
                    put("evening_answers", JSONArray()) // extend with EveningEntry join later
                })
            }
            put("week_entries", weekArray)
        }

        val request = Request.Builder()
            .url("$FLASK_BASE_URL/identity/weekly-report")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = http.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Flask error ${response.code}")

        val json = JSONObject(response.body.string())

        val reportId = UUID.randomUUID().toString()
        weeklyReportDao.insert(
            WeeklyReportEntity(
                id                = reportId,
                uID               = uid,
                weekStart         = Date(startMs),
                overallScore      = json.optDouble("overall_score", 0.0).toFloat(),
                headline          = json.optString("headline"),
                strongestRole     = json.optString("strongest_role"),
                weakestRole       = json.optString("weakest_role"),
                patternIdentified = json.optString("pattern_identified"),
                mirrorInsight     = json.optString("mirror_insight"),
                oneCorrection     = json.optString("one_correction"),
                identityAlignment = json.optString("identity_alignment"),
                generatedAt       = Date(),
                isSynced          = false
            )
        )
        Timber.tag(TAG).i("✔ Weekly report saved")

        val generatedQuestJson = json.optJSONObject("generated_quest")
        if (generatedQuestJson != null) {
            val questMap: Map<String, Any> = gson.fromJson(
                generatedQuestJson.toString(),
                object : TypeToken<Map<String, Any>>() {}.type
            )
            generatedQuestManager.saveQuestFromReport(uid, reportId, questMap)
        }
    }
}