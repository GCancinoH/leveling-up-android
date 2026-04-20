package com.gcancino.levelingup.data.repositories

import android.net.Uri
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.BodyMeasurementDao
import com.gcancino.levelingup.data.local.database.entities.bodyData.BodyCompositionEntity
import com.gcancino.levelingup.data.local.database.entities.bodyData.BodyMeasurementEntity
import com.gcancino.levelingup.data.local.database.mappers.toDomain
import com.gcancino.levelingup.data.local.database.mappers.toEntity
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.bodyComposition.BodyMeasurement
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import jakarta.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async    // Add this
import kotlinx.coroutines.awaitAll // Add this
import kotlinx.coroutines.tasks.await
// ... other imports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

class BodyDataRepositoryImpl @Inject constructor(
    private val compositionDao: BodyCompositionDao,
    private val measurementDao: BodyMeasurementDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,       // ← for photo uploads
    private val auth: FirebaseAuth
) : BodyDataRepository {

    private val TAG = "BodyDataRepository"

    // ─── Composition ──────────────────────────────────────────────────────────────

    override suspend fun saveComposition(composition: BodyComposition): Resource<Unit> {
        return try {
            Timber.tag(TAG).d("Saving composition locally → id: ${composition.id}")
            compositionDao.insert(composition.toEntity())
            Timber.tag(TAG).i("✔ Composition saved to Room (isSynced=false)")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save composition")
            Resource.Error("Failed to save")
        }
    }

    /**
     * Uploads photos to Firebase Storage immediately (can't be batched),
     * then updates the Room record with the returned URLs.
     * Storage path: images/body_composition/{uID}/{timestamp}_{filename}
     */
    override suspend fun uploadCompositionPhotos(
        uID: String,
        recordId: String,
        uris: List<Uri>
    ): Resource<List<String>> {
        return try {
            Timber.tag(TAG).d("Uploading ${uris.size} photo(s) for composition: $recordId")

            val storageRef = storage.reference
            val urls = coroutineScope {
                uris.map { uri ->
                    async {
                        val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
                        val ref = storageRef.child("images/body_composition/$uID/$fileName")
                        ref.putFile(uri).await()
                        ref.downloadUrl.await().toString()
                    }
                }.awaitAll()
            }

            Timber.tag(TAG).d("✔ Uploaded ${urls.size} photo(s) → updating Room record")

            // Update Room record with photo URLs
            val existing = compositionDao.getAll(uID).first()
                .firstOrNull { it.id == recordId }
            if (existing != null) {
                compositionDao.insert(existing.copy(photos = urls))
                Timber.tag(TAG).d("✔ Room record updated with photo URLs")
            }

            Resource.Success(urls)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Photo upload failed")
            Resource.Error("Photo upload failed")
        }
    }

    override fun getCompositionHistory(uID: String): Flow<List<BodyComposition>> =
        compositionDao.getAll(uID).map { it.map { e -> e.toDomain() } }

    override suspend fun hasInitialComposition(uID: String): Boolean =
        compositionDao.countInitialData(uID) > 0

    // ─── Measurements ─────────────────────────────────────────────────────────────

    override suspend fun saveMeasurement(measurement: BodyMeasurement): Resource<Unit> {
        return try {
            Timber.tag(TAG).d("Saving measurement locally → id: ${measurement.id}")
            measurementDao.insert(measurement.toEntity())
            Timber.tag(TAG).i("✔ Measurement saved to Room (isSynced=false)")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save measurement")
            Resource.Error("Failed to save")
        }
    }

    override fun getMeasurementHistory(uID: String): Flow<List<BodyMeasurement>> =
        measurementDao.getAll(uID).map { it.map { e -> e.toDomain() } }

    override suspend fun hasInitialMeasurement(uID: String): Boolean =
        measurementDao.countInitialData(uID) > 0

    // ─── Nightly Sync ─────────────────────────────────────────────────────────────

    override suspend fun syncUnsynced(): Resource<Unit> {
        return try {
            val compositions = compositionDao.getUnsynced()
            val measurements = measurementDao.getUnsynced()

            Timber.tag(TAG).d(
                "syncUnsynced() → ${compositions.size} composition(s), " +
                        "${measurements.size} measurement(s)"
            )

            if (compositions.isEmpty() && measurements.isEmpty()) {
                Timber.tag(TAG).i("Nothing to sync")
                return Resource.Success(Unit)
            }

            val batch = firestore.batch()

            compositions.forEach { entity ->
                val ref = firestore.collection("body_composition").document(entity.id)
                batch.set(ref, entity.toFirestoreMap())
            }

            measurements.forEach { entity ->
                val ref = firestore.collection("player_measurements").document(entity.id)
                batch.set(ref, entity.toFirestoreMap())
            }

            batch.commit().await()
            Timber.tag(TAG).i("✔ Batch committed to Firestore")

            if (compositions.isNotEmpty()) {
                compositionDao.markAsSynced(compositions.map { it.id })
                Timber.tag(TAG).d("✔ ${compositions.size} composition(s) marked synced")
            }
            if (measurements.isNotEmpty()) {
                measurementDao.markAsSynced(measurements.map { it.id })
                Timber.tag(TAG).d("✔ ${measurements.size} measurement(s) marked synced")
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "syncUnsynced() failed")
            Resource.Error("Sync failed")
        }
    }

    // ─── Seed from Firestore (new device) ────────────────────────────────────────

    override suspend fun seedFromFirestore(uID: String): Resource<Unit> {
        return try {
            Timber.tag(TAG).d("Seeding from Firestore for user: $uID")

            val compositions = firestore.collection("body_composition")
                .whereEqualTo("uID", uID)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.data?.toBodyCompositionEntity(doc.id)
                }
            compositions.forEach { compositionDao.insert(it) }
            Timber.tag(TAG).d("Seeded ${compositions.size} composition(s)")

            val measurements = firestore.collection("player_measurements")
                .whereEqualTo("uID", uID)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.data?.toBodyMeasurementEntity(doc.id)
                }
            measurements.forEach { measurementDao.insert(it) }
            Timber.tag(TAG).d("Seeded ${measurements.size} measurement(s)")

            Timber.tag(TAG).i("✔ Firestore seed complete")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "seedFromFirestore() failed")
            Resource.Error("Seed failed")
        }
    }
}

// ─── Firestore Map Helpers ────────────────────────────────────────────────────────

private fun BodyCompositionEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "uID" to uID,
    "date" to Timestamp(date),
    "weight" to weight,
    "bmi" to bmi,
    "bodyFatPercentage" to bodyFatPercentage,
    "muscleMassPercentage" to muscleMassPercentage,
    "visceralFat" to visceralFat,
    "bodyAge" to bodyAge,
    "initialData" to initialData,
    "photos" to photos,
    "unitSystem" to unitSystem
)

private fun BodyMeasurementEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "uID" to uID,
    "date" to Timestamp(date),
    "neck" to neck,
    "shoulders" to shoulders,
    "chest" to chest,
    "waist" to waist,
    "umbilical" to umbilical,
    "hip" to hip,
    "bicepLeftRelaxed" to bicepLeftRelaxed,
    "bicepLeftFlexed" to bicepLeftFlexed,
    "bicepRightRelaxed" to bicepRightRelaxed,
    "bicepRightFlexed" to bicepRightFlexed,
    "forearmLeft" to forearmLeft,
    "forearmRight" to forearmRight,
    "thighLeft" to thighLeft,
    "thighRight" to thighRight,
    "calfLeft" to calfLeft,
    "calfRight" to calfRight,
    "initialData" to initialData,
    "unitSystem" to unitSystem
)

private fun Map<String, Any>.toBodyCompositionEntity(id: String) = BodyCompositionEntity(
    id = id,
    uID = this["uID"] as? String ?: "",
    date = (this["date"] as? Timestamp)?.toDate() ?: Date(),
    weight = (this["weight"] as? Number)?.toDouble() ?: 0.0,
    bmi = (this["bmi"] as? Number)?.toDouble() ?: 0.0,
    bodyFatPercentage = (this["bodyFatPercentage"] as? Number)?.toDouble() ?: 0.0,
    muscleMassPercentage = (this["muscleMassPercentage"] as? Number)?.toDouble() ?: 0.0,
    visceralFat = (this["visceralFat"] as? Number)?.toDouble() ?: 0.0,
    bodyAge = (this["bodyAge"] as? Number)?.toInt() ?: 0,
    initialData  = this["initialData"] as? Boolean ?: false,
    photos = (this["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
    unitSystem = this["unitSystem"] as? String ?: "METRIC",
    isSynced = true  // came from Firestore → already synced
)

private fun Map<String, Any>.toBodyMeasurementEntity(id: String) = BodyMeasurementEntity(
    id               = id,
    uID              = this["uID"] as? String ?: "",
    date             = (this["date"] as? Timestamp)?.toDate() ?: Date(),
    neck             = (this["neck"] as? Number)?.toDouble() ?: 0.0,
    shoulders        = (this["shoulders"] as? Number)?.toDouble() ?: 0.0,
    chest            = (this["chest"] as? Number)?.toDouble() ?: 0.0,
    waist            = (this["waist"] as? Number)?.toDouble() ?: 0.0,
    umbilical        = (this["umbilical"] as? Number)?.toDouble() ?: 0.0,
    hip              = (this["hip"] as? Number)?.toDouble() ?: 0.0,
    bicepLeftRelaxed  = (this["bicepLeftRelaxed"] as? Number)?.toDouble() ?: 0.0,
    bicepLeftFlexed   = (this["bicepLeftFlexed"] as? Number)?.toDouble() ?: 0.0,
    bicepRightRelaxed = (this["bicepRightRelaxed"] as? Number)?.toDouble() ?: 0.0,
    bicepRightFlexed  = (this["bicepRightFlexed"] as? Number)?.toDouble() ?: 0.0,
    forearmLeft      = (this["forearmLeft"] as? Number)?.toDouble() ?: 0.0,
    forearmRight     = (this["forearmRight"] as? Number)?.toDouble() ?: 0.0,
    thighLeft        = (this["thighLeft"] as? Number)?.toDouble() ?: 0.0,
    thighRight       = (this["thighRight"] as? Number)?.toDouble() ?: 0.0,
    calfLeft         = (this["calfLeft"] as? Number)?.toDouble() ?: 0.0,
    calfRight        = (this["calfRight"] as? Number)?.toDouble() ?: 0.0,
    initialData      = this["initialData"] as? Boolean ?: false,
    unitSystem       = this["unitSystem"] as? String ?: "METRIC",
    isSynced         = true
)