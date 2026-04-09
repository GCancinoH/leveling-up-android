package com.gcancino.levelingup.data.repositories

import android.content.Context
import android.net.Uri
import com.gcancino.levelingup.core.Config
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.NutritionEntryDao
import com.gcancino.levelingup.data.local.database.entities.NutritionEntryEntity
import com.gcancino.levelingup.domain.logic.TimeProvider
import com.gcancino.levelingup.domain.models.nutrition.*
import com.gcancino.levelingup.domain.repositories.NutritionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NutritionRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val nutritionDao: NutritionEntryDao,
    private val timeProvider: TimeProvider,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val gson: Gson
) : NutritionRepository {

    private val TAG = "NutritionRepository"

    // ← Change to your Flask server URL
    private val FLASK_BASE_URL = Config.BACKEND_ENDPOINT

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)  // Vision models take longer
        .build()

    // ─── Analyze food photo ───────────────────────────────────────────────────

    override suspend fun analyzeFood(
        uID: String,
        imageUri: Uri,
        identityStatement: String,
        nutritionStandardTitles: List<String>
    ): Resource<NutritionEntry> {
        return try {
            // 1. Copy URI to temp file (OkHttp needs a File)
            val tempFile = copyUriToTempFile(imageUri)
                ?: return Resource.Error("Failed to read image")

            // 2. Build multipart request
            val standardsJson = gson.toJson(
                nutritionStandardTitles.mapIndexed { i, title ->
                    mapOf("id" to "s$i", "title" to title)
                }
            )

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image", tempFile.name,
                    tempFile.asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("identity_statement", identityStatement)
                .addFormDataPart("nutrition_standards", standardsJson)
                .build()

            val request = Request.Builder()
                .url("$FLASK_BASE_URL/nutrition/analyze")
                .post(requestBody)
                .build()

            // 3. Call Flask
            val response = http.newCall(request).execute()
            tempFile.delete()

            if (!response.isSuccessful) {
                return Resource.Error("Analysis failed: ${response.code}")
            }

            val json = JSONObject(response.body.string())

            // 4. Upload photo to Firebase Storage (async, non-blocking)
            val photoUrl = uploadPhotoToStorage(uID, imageUri)

            // 5. Parse response and save to Room
            val entry = parseAndSave(uID, json, photoUrl)
            Timber.tag(TAG).i("✔ Food analyzed → ${entry.foodIdentified} | ${entry.alignment}")

            Resource.Success(entry)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "analyzeFood() failed")
            Resource.Error(e.message ?: "Analysis failed")
        }
    }

    // ─── Observations ─────────────────────────────────────────────────────────

    override fun observeTodayEntries(uID: String): Flow<List<NutritionEntry>> {
        val (start, end) = timeProvider.dayBoundaries(timeProvider.today())
        return nutritionDao.observeForDay(uID, start, end)
            .map { it.map { e -> e.toDomain() } }
    }

    override fun observeTodayMacros(uID: String): Flow<MacroSummary> {
        return observeTodayEntries(uID).map { entries ->
            entries.fold(MacroSummary(0, 0f, 0f, 0f, 0f)) { acc, e -> acc + e.macroSummary }
        }
    }

    override fun observeAlignedCountToday(uID: String): Flow<Int> {
        val (start, end) = timeProvider.dayBoundaries(timeProvider.today())
        return nutritionDao.countAlignedForDay(uID, start, end)
    }

    override fun observeRecentEntries(uID: String): Flow<List<NutritionEntry>> =
        nutritionDao.observeRecent(uID).map { it.map { e -> e.toDomain() } }

    // ─── Sync ─────────────────────────────────────────────────────────────────

    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val unsynced = nutritionDao.getUnsynced()
            if (unsynced.isEmpty()) return Resource.Success(Unit)

            val batch = firestore.batch()
            unsynced.forEach { entry ->
                val ref = firestore.collection("nutrition_entries").document(entry.id)
                batch.set(ref, entry.toFirestoreMap())
            }
            batch.commit().await()
            nutritionDao.markAsSynced(unsynced.map { it.id })
            Timber.tag(TAG).i("✔ Nutrition sync complete")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced() failed")
            Resource.Error(e.message ?: "Sync failed")
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun parseAndSave(uID: String, json: JSONObject, photoUrl: String): NutritionEntry {
        val macros = json.optJSONObject("macros")
        val micros = json.optJSONObject("micros")

        val highlights: List<String> = micros?.optJSONArray("highlights")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        val concerns: List<String> = micros?.optJSONArray("concerns")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()

        val entry = NutritionEntry(
            id              = UUID.randomUUID().toString(),
            uID             = uID,
            date            = Date(),
            foodIdentified  = json.optString("food_identified", "Unknown food"),
            photoUrl        = photoUrl,
            calories        = json.optInt("total_calories_computed", 0), // Python-computed
            proteinG        = macros?.optDouble("protein_g", 0.0)?.toFloat() ?: 0f,
            carbsG          = macros?.optDouble("carbs_g", 0.0)?.toFloat() ?: 0f,
            fatsG           = macros?.optDouble("fats_g", 0.0)?.toFloat() ?: 0f,
            fiberG          = macros?.optDouble("fiber_g", 0.0)?.toFloat() ?: 0f,
            microHighlights = highlights,
            microConcerns   = concerns,
            processingLevel = parseProcessingLevel(json.optString("processing_level")),
            alignment       = parseAlignment(json.optString("alignment")),
            alignmentReason = json.optString("alignment_reason", ""),
            suggestion      = json.optString("suggestion", ""),
            isSynced        = false
        )

        // Fire-and-forget Room insert — caller collects from Flow
        kotlinx.coroutines.runBlocking {
            nutritionDao.insert(entry.toEntity())
        }

        return entry
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile    = File.createTempFile("food_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "copyUriToTempFile() failed")
            null
        }
    }

    private suspend fun uploadPhotoToStorage(uID: String, uri: Uri): String {
        return try {
            val ref  = storage.reference.child("nutrition/$uID/${UUID.randomUUID()}.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Timber.tag(TAG).w("Photo upload failed (non-fatal): ${e.message}")
            "" // non-fatal — macros still saved
        }
    }

    private fun parseAlignment(value: String) = when (value.lowercase()) {
        "aligned"    -> NutritionAlignment.ALIGNED
        "misaligned" -> NutritionAlignment.MISALIGNED
        else         -> NutritionAlignment.NEUTRAL
    }

    private fun parseProcessingLevel(value: String) = when (value.lowercase()) {
        "whole_food"           -> ProcessingLevel.WHOLE_FOOD
        "minimally_processed"  -> ProcessingLevel.MINIMALLY_PROCESSED
        "ultra_processed"      -> ProcessingLevel.ULTRA_PROCESSED
        else                   -> ProcessingLevel.UNKNOWN
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private fun NutritionEntry.toEntity() = NutritionEntryEntity(
        id = id,
        uID = uID,
        date = date,
        foodIdentified = foodIdentified,
        photoUrl = photoUrl,
        calories = calories,
        proteinG = proteinG,
        carbsG = carbsG,
        fatsG = fatsG,
        fiberG = fiberG,
        microHighlights = gson.toJson(microHighlights),
        microConcerns = gson.toJson(microConcerns),
        processingLevel = processingLevel.name,
        alignment = alignment.name,
        alignmentReason = alignmentReason,
        suggestion = suggestion,
        isSynced = false
    )

    private fun NutritionEntryEntity.toDomain(): NutritionEntry {
        val listType = object : TypeToken<List<String>>() {}.type
        return NutritionEntry(
            id              = id,
            uID             = uID,
            date            = date,
            foodIdentified  = foodIdentified,
            photoUrl        = photoUrl,
            calories        = calories,
            proteinG        = proteinG,
            carbsG          = carbsG,
            fatsG           = fatsG,
            fiberG          = fiberG,
            microHighlights = gson.fromJson(microHighlights, listType),
            microConcerns   = gson.fromJson(microConcerns, listType),
            processingLevel = parseProcessingLevel(processingLevel),
            alignment       = parseAlignment(alignment),
            alignmentReason = alignmentReason,
            suggestion      = suggestion,
            isSynced        = isSynced
        )
    }

    private fun NutritionEntryEntity.toFirestoreMap() = mapOf(
        "id"              to id,
        "uID"             to uID,
        "date"            to date,
        "foodIdentified"  to foodIdentified,
        "photoUrl"        to photoUrl,
        "calories"        to calories,
        "proteinG"        to proteinG,
        "carbsG"          to carbsG,
        "fatsG"           to fatsG,
        "fiberG"          to fiberG,
        "alignment"       to alignment,
        "alignmentReason" to alignmentReason,
        "isSynced"        to true
    )
}