package com.gcancino.levelingup.data.repositories

import android.content.Context
import android.net.Uri
import com.gcancino.levelingup.core.Config
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.NutritionEntryDao
import com.gcancino.levelingup.data.local.database.entities.NutritionEntryEntity
import com.gcancino.levelingup.data.network.NutritionApiService
import com.gcancino.levelingup.data.network.dto.NutritionResponseDto
import com.gcancino.levelingup.domain.logic.TimeProvider
import com.gcancino.levelingup.domain.models.nutrition.*
import com.gcancino.levelingup.domain.repositories.NutritionRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject

class NutritionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nutritionDao: NutritionEntryDao,
    private val timeProvider: TimeProvider,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val apiService: NutritionApiService
) : NutritionRepository {

    private val TAG = "NutritionRepository"
    private val json = Json { ignoreUnknownKeys = true }

    // ← Backend URL from centralized Config
    private val FLASK_BASE_URL = Config.BACKEND_ENDPOINT

    // ─── Analyze food photo ───────────────────────────────────────────────────
    override suspend fun analyzeFood(
        uID: String,
        imageUri: Uri,
        identityStatement: String,
        nutritionStandards: List<NutritionStandardDto>  // ← cambio de firma
    ): Resource<NutritionEntry>  {
        return analyzeFoodImpl(uID, imageUri, identityStatement, nutritionStandards)
    }

    private suspend fun analyzeFoodImpl(
        uID: String,
        imageUri: Uri,
        identityStatement: String,
        nutritionStandards: List<NutritionStandardDto>
    ): Resource<NutritionEntry> {
        return try {
            val tempFile = copyUriToTempFile(imageUri)
                ?: return Resource.Error("Failed to read image")

            // FIX 1: standards llegan con IDs reales, no índices generados
            val standardsJson = json.encodeToString(nutritionStandards.map {
                mapOf("id" to it.id, "title" to it.title)
            })

            val imagePart = MultipartBody.Part.createFormData(
                "image", 
                tempFile.name,
                tempFile.asRequestBody("image/jpeg".toMediaType())
            )
            val identityPart = identityStatement.toRequestBody("text/plain".toMediaType())
            val standardsPart = standardsJson.toRequestBody("application/json".toMediaType())

            val responseDto = apiService.analyzeFood(
                image = imagePart,
                identityStatement = identityPart,
                nutritionStandards = standardsPart
            )
            tempFile.delete()

            val photoUrl = uploadPhotoToStorage(uID, imageUri)
            val entry    = parseAndSave(uID, responseDto, photoUrl)
            Timber.tag(TAG).i("✔ Food analyzed → ${entry.foodIdentified} | score: ${entry.alignmentScore}")
            Resource.Success(entry)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "analyzeFood() failed")
            Resource.Error("Analysis failed: ${e.message}")
        }
    }

    // ─── Observations ─────────────────────────────────────────────────────────
    @ExperimentalCoroutinesApi
    override fun observeTodayData(uID: String): Flow<TodayNutritionData> {
        return flow {
            while (true) {
                emit(timeProvider.dayBoundaries(timeProvider.today()))
                delay(60_000L) // 1 minute ticker to recalculate day boundaries avoiding midnight staleness
            }
        }.flatMapLatest { (start, end) ->
            nutritionDao.observeForDay(uID, start, end).map { list ->
                val entries = list.map { it.toDomain() }
                val macros = entries.fold(MacroSummary(0, 0f, 0f, 0f, 0f)) { acc, e -> acc + e.macroSummary }
                TodayNutritionData(entries, macros)
            }
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
            Resource.Error("Sync failed")
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private suspend fun parseAndSave(uID: String, dto: NutritionResponseDto, photoUrl: String): NutritionEntry {
        // FIX 3 — parse action from DTO
        val action: NutritionAction? = dto.action?.let { actionDto ->
            val type = try { NutritionActionType.valueOf(actionDto.type) }
            catch (e: Exception) { NutritionActionType.NONE }

            NutritionAction(
                type       = type,
                taskTitle  = actionDto.payload?.taskTitle?.takeIf { it.isNotBlank() && it != "null" },
                standardId = actionDto.payload?.standardId?.takeIf { it.isNotBlank() && it != "null" },
                message    = actionDto.payload?.message?.takeIf { it.isNotBlank() && it != "null" }
            )
        }

        val entry = NutritionEntry(
            id              = UUID.randomUUID().toString(),
            uID             = uID,
            date            = Date(),
            foodIdentified  = dto.foodIdentified,
            photoUrl        = photoUrl,
            calories        = dto.totalCalories,
            proteinG        = dto.macros.proteinG,
            carbsG          = dto.macros.carbsG,
            fatsG           = dto.macros.fatsG,
            fiberG          = dto.macros.fiberG,
            microHighlights = dto.micros.highlights,
            microConcerns   = dto.micros.concerns,
            processingLevel = parseProcessingLevel(dto.processingLevel),
            alignment       = parseAlignment(dto.alignment),
            alignmentReason = dto.alignmentReason,
            suggestion      = dto.suggestion,
            alignmentScore  = dto.alignmentScore,
            action          = action,
            isSynced        = false
        )

        // TODO: P2 fix blocked by Kotlin 2.0 annotation targeting issue with Room
        nutritionDao.insert(entry.toEntity())
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
        microHighlights = json.encodeToString(microHighlights),
        microConcerns   = json.encodeToString(microConcerns),
        processingLevel = processingLevel.name,
        alignment       = alignment.name,
        alignmentReason = alignmentReason,
        suggestion      = suggestion,
        alignmentScore  = alignmentScore,       // FIX 2
        action          = action?.let { json.encodeToString(it) } ?: "",  // FIX 3
        isSynced        = false
    )

    private fun NutritionEntryEntity.toDomain(): NutritionEntry {
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
            microHighlights = json.decodeFromString(microHighlights),
            microConcerns   = json.decodeFromString(microConcerns),
            processingLevel = parseProcessingLevel(processingLevel),
            alignment       = parseAlignment(alignment),
            alignmentReason = alignmentReason,
            suggestion      = suggestion,
            alignmentScore  = alignmentScore,   // FIX 2
            action          = if (action.isNotBlank())
                json.decodeFromString<NutritionAction>(action)
            else null,        // FIX 3
            isSynced        = isSynced
        )
    }

    private fun NutritionEntryEntity.toFirestoreMap(): Map<String, Any?> {
        val actionObj = if (action.isNotBlank()) json.decodeFromString<NutritionAction>(action) else null

        return mapOf(
            "id" to id,
            "uID" to uID,
            "date" to date,
            "foodIdentified" to foodIdentified,
            "photoUrl" to photoUrl,
            "calories" to calories,
            "proteinG" to proteinG,
            "carbsG" to carbsG,
            "fatsG" to fatsG,
            "fiberG" to fiberG,
            "microHighlights" to json.decodeFromString<List<String>>(microHighlights),
            "microConcerns" to json.decodeFromString<List<String>>(microConcerns),
            "processingLevel" to processingLevel,
            "alignment" to alignment,
            "alignmentReason" to alignmentReason,
            "suggestion" to suggestion,
            "alignmentScore" to alignmentScore,
            "action" to actionObj?.let {
                mapOf(
                    "type" to it.type.name,
                    "taskTitle" to it.taskTitle,
                    "standardId" to it.standardId,
                    "message" to it.message
                )
            },
            "isSynced" to true
        )
    }
}