package com.gcancino.levelingup.data.repositories

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.domain.models.BodyComposition
import com.gcancino.levelingup.domain.repositories.BodyCompositionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BodyCompositionRepositoryImpl @Inject constructor(
    private val bodyCompositionDao: BodyCompositionDao,
    private val db: FirebaseFirestore,
    private val storageDB: FirebaseStorage,
    private val auth: FirebaseAuth
) : BodyCompositionRepository {

    override fun saveBodyComposition(): Flow<Resource<Unit>> {
        TODO("Not yet implemented")
    }

    override fun saveBodyCompositionLocally(): Flow<Resource<Unit>> {
        TODO("Not yet implemented")
    }

    override fun saveInitialBodyComposition(data: BodyComposition): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        try {
            val localOutcome = withContext(Dispatchers.IO) {
                bodyCompositionDao.updateInitialBodyComposition(
                    uid = data.uid,
                    bodyFat = data.bodyFat!!,
                    muscleMass = data.muscleMass!!,
                    visceralFat = data.visceralFat!!,
                    bodyAge = data.bodyAge!!
                )
            }

            if (localOutcome <= 0) {
                emit(Resource.Error("Failed to update local body composition"))
                return@flow
            }

            // Firestore Update
            coroutineScope {
                launch {
                    runCatching {
                        val bodyCompositionRef = db.collection("body_composition")
                        val querySnapshot = bodyCompositionRef
                            .whereEqualTo("uid", data.uid)
                            .whereEqualTo("isInitial", true)
                            .limit(1)
                            .get()
                            .await()

                        if (!querySnapshot.isEmpty) {
                            val documentToUpdate = querySnapshot.documents[0]
                            val updates = hashMapOf<String, Any>(
                                "bodyFat" to data.bodyFat!!,
                                "muscleMass" to data.muscleMass!!,
                                "visceralFat" to data.visceralFat!!,
                                "bodyAge" to data.bodyAge!!
                            )
                            bodyCompositionRef.document(documentToUpdate.id)
                                .update(updates)
                                .await()
                        } else {
                            println("Document don't exists")
                        }
                    }.onFailure { exception ->
                        // Log error but don't fail the operation since local save succeeded
                        println("Firestore sync failed: ${exception.message}")
                    }
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown Error"))
        }
    }

    override fun updateInitialBodyCompositionPhotos(data: List<Uri>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        val storageRef = storageDB.reference
        val user = auth.currentUser
        val photosUris = mutableListOf<String>()

        try {
            coroutineScope {
                val uploadTask = data.map { uri ->
                    async {
                        val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment}"
                        val ref = storageRef.child("images/body_composition/${user!!.uid}/$fileName")

                        ref.putFile(uri).await()
                        val downloadUrl = ref.downloadUrl.await()
                        downloadUrl.toString()
                    }
                }

                val uploadedUrls = uploadTask.awaitAll()
                photosUris.addAll(uploadedUrls)
            }

            // Update local database
            val localOutcome = withContext(Dispatchers.IO) {
                bodyCompositionDao.updateInitialPhotos(
                    uid = user!!.uid,
                    photos = photosUris
                )
            }

            if (localOutcome <= 0) {
                emit(Resource.Error("Failed to update local body composition"))
                return@flow
            }

            // Update firestore in background
            coroutineScope {
                launch {
                    runCatching {
                        val bodyCompositionRef = db.collection("body_composition")
                            .document(user!!.uid)
                        val updatedData = hashMapOf<String, Any>(
                            "photos" to photosUris
                        )
                        bodyCompositionRef.update(updatedData).await()
                    }.onFailure { exception ->
                        // Log error but don't fail the operation since local save succeeded
                        emit(Resource.Error("Firestore sync failed: ${exception.message}"))
                    }
                }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown Error"))
        }
    }


}