package com.gcancino.levelingup.data.repositories

import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.ExerciseDao
import com.gcancino.levelingup.data.local.database.entities.ExerciseBlockEntity
import com.gcancino.levelingup.data.local.database.entities.ExerciseEntity
import com.gcancino.levelingup.data.local.database.entities.ExerciseSetEntity
import com.gcancino.levelingup.data.local.database.entities.SetLogEntity
import com.gcancino.levelingup.data.local.database.entities.TrainingSessionEntity
import com.gcancino.levelingup.domain.models.exercise.*
import com.gcancino.levelingup.domain.repositories.ExerciseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlinx.serialization.json.*
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    private val TAG = "ExerciseRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ─── JSON Helpers ─────────────────────────────────────────────────────────────

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Timestamp -> JsonPrimitive(this.toDate().time)
        is Date -> JsonPrimitive(this.time)
        is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
        is List<*> -> JsonArray(map { it.toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }

    private inline fun <reified T> Map<String, Any>.toSerializableObject(): T =
        json.decodeFromJsonElement(this.toJsonElement())

    // ─── syncWeek (no-op — Firestore is source of truth) ─────────────────────────

    override suspend fun syncWeek(date: LocalDate): Resource<Unit> {
        Timber.tag(TAG).d("syncWeek() → no-op, Firestore is source of truth")
        return Resource.Success(Unit)
    }

    // ─── Session Reads (Firestore) ────────────────────────────────────────────────

    override fun getTodaysSession(): Flow<Resource<TrainingSession?>> {
        Timber.tag(TAG).d("getTodaysSession() called")
        return getSessionForDate(Date())
    }

    override fun getSessionForDate(date: Date): Flow<Resource<TrainingSession?>> = flow {
        emit(Resource.Loading())
        Timber.tag(TAG).d("getSessionForDate() → querying Firestore for: $date")

        try {
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = cal.time

            Timber.tag(TAG).d("Firestore query window → $startOfDay to $endOfDay")

            val snapshot = firestore.collection("sessions")
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endOfDay)
                .limit(1)
                .get()
                .await()

            Timber.tag(TAG).d("Firestore returned ${snapshot.documents.size} session(s)")

            val sessionDoc = snapshot.documents.firstOrNull()
            if (sessionDoc == null) {
                Timber.tag(TAG).w("✘ No session for $date → emitting null")
                emit(Resource.Success(null))
                return@flow
            }

            val data = sessionDoc.data
            if (data == null) {
                Timber.tag(TAG).w("Session ${sessionDoc.id} has null data")
                emit(Resource.Success(null))
                return@flow
            }

            Timber.tag(TAG).d("Session found → id: ${sessionDoc.id}")
            var session = data.toSerializableObject<TrainingSession>().copy(id = sessionDoc.id)

            val blocksSnapshot = firestore
                .collection("sessions")
                .document(sessionDoc.id)
                .collection("blocks")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            Timber.tag(TAG).d("Blocks fetched: ${blocksSnapshot.documents.size}")

            val blocks = blocksSnapshot.documents.mapNotNull { blockDoc ->
                val blockData = blockDoc.data ?: run {
                    Timber.tag(TAG).w("Block ${blockDoc.id} has null data — skipping")
                    return@mapNotNull null
                }
                blockData.toSerializableObject<ExerciseBlock>().copy(
                    id        = blockDoc.id,
                    sessionId = sessionDoc.id
                )
            }

            session = session.copy(blocks = blocks)

            Timber.tag(TAG).i(
                "✔ Session assembled → name: '${session.name}' | " +
                        "blocks: ${blocks.size} | exercises: ${blocks.sumOf { it.exercises.size }}"
            )

            emit(Resource.Success(session))

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "getSessionForDate() failed for: $date")
            emit(Resource.Error("Error loading session"))
        }
    }

    // ─── Set Logs (Room) ──────────────────────────────────────────────────────────

    override suspend fun toggleSetLog(
        sessionId: String,
        exerciseId: String,
        setIndex: Int,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            Timber.tag(TAG).d("Inserting set log → session: $sessionId | exercise: $exerciseId | set: $setIndex")
            exerciseDao.insertSetLog(
                SetLogEntity(
                    sessionId    = sessionId,
                    exerciseId   = exerciseId,
                    setIndex     = setIndex,
                    actualReps   = null,
                    actualWeight = null,
                    isCompleted  = true
                )
            )
        } else {
            Timber.tag(TAG).d("Deleting set log → session: $sessionId | exercise: $exerciseId | set: $setIndex")
            exerciseDao.deleteSetLog(sessionId, exerciseId, setIndex)
        }
    }

    override fun getCompletedSetsForSession(sessionId: String): Flow<Set<Int>> {
        return exerciseDao.getLogsForSession(sessionId).map { logs ->
            logs.map { it.setIndex }.toSet()
        }
    }

    /**
     * ── #12/#14 fix: returns the full per-exercise breakdown needed to restore
     * _completedSets in the ViewModel after app relaunch.
     *
     * Groups SetLogEntity rows by exerciseId and maps each group to a Set<Int>
     * of completed setIndexes — exactly the shape of _completedSets.
     *
     * Flow stays hot so the UI reflects any Room changes immediately.
     */
    override fun getLogsForSessionAsMap(sessionId: String): Flow<Map<String, Set<Int>>> {
        Timber.tag(TAG).d("getLogsForSessionAsMap() → session: $sessionId")
        return exerciseDao.getLogsForSession(sessionId).map { logs ->
            logs
                .groupBy { it.exerciseId }
                .mapValues { (_, entries) -> entries.map { it.setIndex }.toSet() }
                .also {
                    Timber.tag(TAG).d(
                        "Restored completed sets map → " +
                                "${it.values.sumOf { set -> set.size }} total sets across ${it.size} exercise(s)"
                    )
                }
        }
    }

    // ─── 1RM (Firestore real-time) ────────────────────────────────────────────────

    override fun getOneRepMaxes(): Flow<Resource<List<OneRepMax>>> = callbackFlow {
        trySend(Resource.Loading())
        val userId = auth.currentUser?.uid ?: "84tDYVTSukQ0UcuuqqaNLytXcQv1"
        Timber.tag(TAG).d("Subscribing to 1RM listener for user: $userId")

        val registration = firestore.collection("exercises_1rm")
            .whereEqualTo("uID", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag(TAG).e(error, "1RM snapshot error")
                    trySend(Resource.Error(error.message ?: "Error fetching 1RM"))
                    return@addSnapshotListener
                }
                val oneRepMaxes = snapshot?.documents?.flatMap { doc ->
                    (doc.data ?: emptyMap())
                        .filter { (key, value) -> key != "uID" && key != "date" && value is Number }
                        .map { (key, value) ->
                            OneRepMax(
                                id           = doc.id,
                                userId       = userId,
                                exerciseId   = key,
                                exerciseName = key,
                                weight       = (value as Number).toDouble()
                            )
                        }
                } ?: emptyList()
                Timber.tag(TAG).d("1RM update → ${oneRepMaxes.size} entries")
                trySend(Resource.Success(oneRepMaxes))
            }

        awaitClose {
            Timber.tag(TAG).d("Removing 1RM listener")
            registration.remove()
        }
    }

    override suspend fun saveTodaySessionLocally(session: TrainingSession): Resource<Unit> {
        return try {
            Timber.tag(TAG).d("saveTodaySessionLocally() → saving session: ${session.id}")

            // Insert session
            exerciseDao.insertTrainingSessions(listOf(
                TrainingSessionEntity(
                    id           = session.id,
                    completed    = session.completed,
                    macrocycleId = session.macrocycleId,
                    mesocycleId  = session.mesocycleId,
                    microcycleId = session.microcycleId,
                    date         = session.date,
                    name         = session.name
                )
            ))
            Timber.tag(TAG).d("✔ Session inserted into Room")

            // Insert blocks
            val blockEntities = session.blocks.map { block ->
                ExerciseBlockEntity(
                    id = block.id,
                    sessionId = session.id,
                    type = block.type,
                    sets = block.sets,
                    restBetweenExercises = block.restBetweenExercises,
                    restAfterBlock = block.restAfterBlock,
                    order = block.order
                )
            }
            exerciseDao.insertExerciseBlocks(blockEntities)
            Timber.tag(TAG).d("✔ ${blockEntities.size} block(s) inserted into Room")

            // Insert exercises and sets
            val exerciseEntities = mutableListOf<ExerciseEntity>()
            val setEntities      = mutableListOf<ExerciseSetEntity>()

            session.blocks.forEach { block ->
                block.exercises.forEachIndexed { exIdx, exercise ->
                    exerciseEntities.add(
                        ExerciseEntity(
                            id      = exercise.id,
                            blockId = block.id,
                            name    = exercise.name,
                            role    = exercise.exerciseRole,
                            notes   = exercise.notes,
                            order   = exIdx
                        )
                    )
                    exercise.sets.forEachIndexed { setIdx, set ->
                        setEntities.add(
                            ExerciseSetEntity(
                                id            = set.id,
                                exerciseId    = exercise.id,
                                reps          = set.reps,
                                intensity     = set.intensity,
                                intensityType = set.intensityType,
                                restSeconds   = set.restSeconds,
                                order         = setIdx
                            )
                        )
                    }
                }
            }

            exerciseDao.insertExercises(exerciseEntities)
            Timber.tag(TAG).d("✔ ${exerciseEntities.size} exercise(s) inserted into Room")

            exerciseDao.insertExerciseSets(setEntities)
            Timber.tag(TAG).d("✔ ${setEntities.size} set(s) inserted into Room")

            Timber.tag(TAG).i("✔ Today's session fully saved locally")
            Resource.Success(Unit)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "saveTodaySessionLocally() failed")
            Resource.Error("Failed to save session locally")
        }
    }
}