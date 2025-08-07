package com.gcancino.levelingup.domain.repositories

import android.net.Uri
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.BodyComposition
import kotlinx.coroutines.flow.Flow

interface BodyCompositionRepository {
    fun saveBodyComposition(): Flow<Resource<Unit>>
    fun saveBodyCompositionLocally(): Flow<Resource<Unit>>

    fun saveInitialBodyComposition(data: BodyComposition): Flow<Resource<Unit>>

    fun updateInitialBodyCompositionPhotos(data: List<Uri>) : Flow<Resource<Unit>>
}