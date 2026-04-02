package com.gcancino.levelingup.domain.repositories

import android.net.Uri
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.bodyComposition.BodyMeasurement
import kotlinx.coroutines.flow.Flow

interface BodyDataRepository {
    suspend fun saveComposition(composition: BodyComposition): Resource<Unit>
    suspend fun uploadCompositionPhotos(uID: String, recordId: String, uris: List<Uri>): Resource<List<String>>
    fun getCompositionHistory(uID: String): Flow<List<BodyComposition>>
    suspend fun hasInitialComposition(uID: String): Boolean

    suspend fun saveMeasurement(measurement: BodyMeasurement): Resource<Unit>
    fun getMeasurementHistory(uID: String): Flow<List<BodyMeasurement>>
    suspend fun hasInitialMeasurement(uID: String): Boolean

    suspend fun syncUnsynced(): Resource<Unit>
    suspend fun seedFromFirestore(uID: String): Resource<Unit>
}