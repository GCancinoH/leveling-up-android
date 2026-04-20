package com.gcancino.levelingup.presentation.onboarding

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.BodyMeasurementDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.domain.models.onBoarding.OnboardingData
import com.gcancino.levelingup.domain.models.onBoarding.OnboardingPrefs
import com.gcancino.levelingup.domain.models.onBoarding.OnboardingStep
import com.gcancino.levelingup.domain.repositories.BodyDataRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import androidx.core.content.edit
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.bodyComposition.BodyMeasurement
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val application: Application,
    private val auth: FirebaseAuth,
    private val playerDao: PlayerDao,
    private val compositionDao: BodyCompositionDao,
    private val measurementDao: BodyMeasurementDao,
    private val bodyDataRepository: BodyDataRepository
) : AndroidViewModel(application) {

    private val TAG = "OnboardingViewModel"

    // ─── SharedPreferences for step resume ────────────────────────────────────────

    private val prefs = application.getSharedPreferences(
        OnboardingPrefs.PREFS_NAME, Context.MODE_PRIVATE
    )

    // ─── Step navigation ──────────────────────────────────────────────────────────

    private val _currentStep = MutableStateFlow(
        OnboardingStep.fromIndex(
            prefs.getInt(OnboardingPrefs.KEY_CURRENT_STEP, 0)
        )
    )
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    val progress: StateFlow<Float> = _currentStep
        .map { step ->
            if (step.index == 0) 0f
            else step.index.toFloat() / (OnboardingStep.totalSteps - 1).toFloat()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // ─── In-memory onboarding data ────────────────────────────────────────────────

    private val _data = MutableStateFlow(OnboardingData())
    val data: StateFlow<OnboardingData> = _data.asStateFlow()

    // ─── Save state ───────────────────────────────────────────────────────────────

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState.asStateFlow()

    // ─── Step navigation ──────────────────────────────────────────────────────────

    fun nextStep() {
        val next = OnboardingStep.fromIndex(_currentStep.value.index + 1)
        _currentStep.value = next
        persistCurrentStep(next)
        Timber.tag(TAG).d("→ Step ${next.index}: ${next.name}")
    }

    fun previousStep() {
        val prev = OnboardingStep.fromIndex((_currentStep.value.index - 1).coerceAtLeast(0))
        _currentStep.value = prev
        persistCurrentStep(prev)
        Timber.tag(TAG).d("← Step ${prev.index}: ${prev.name}")
    }

    private fun persistCurrentStep(step: OnboardingStep) {
        prefs.edit { putInt(OnboardingPrefs.KEY_CURRENT_STEP, step.index) }
    }

    // ─── Data update functions (called by each step) ──────────────────────────────

    fun updatePersonalInfo(name: String, birthDate: Date?, gender: Genders?) {
        _data.value = _data.value.copy(
            displayName = name,
            birthDate   = birthDate,
            gender      = gender
        )
        Timber.tag(TAG).d("Personal info updated → name: $name | gender: $gender")
    }

    fun updatePhysicalAndComposition(
        height: Double, weight: Double, bmi: Double,
        bodyFat: Double, muscleMass: Double,
        visceralFat: Double, bodyAge: Int,
        unitSystem: UnitSystem
    ) {
        _data.value = _data.value.copy(
            height               = height,
            weight               = weight,
            bmi                  = bmi,
            bodyFatPercentage    = bodyFat,
            muscleMassPercentage = muscleMass,
            visceralFat          = visceralFat,
            bodyAge              = bodyAge,
            unitSystem           = unitSystem
        )
        Timber.tag(TAG).d("Physical + composition updated → weight: $weight | bmi: $bmi")
    }

    fun updateMeasurements(
        neck: Double, shoulders: Double, chest: Double,
        waist: Double, umbilical: Double, hip: Double,
        bicepLeftRelaxed: Double, bicepLeftFlexed: Double,
        bicepRightRelaxed: Double, bicepRightFlexed: Double,
        forearmLeft: Double, forearmRight: Double,
        thighLeft: Double, thighRight: Double,
        calfLeft: Double, calfRight: Double
    ) {
        _data.value = _data.value.copy(
            neck              = neck,
            shoulders         = shoulders,
            chest             = chest,
            waist             = waist,
            umbilical         = umbilical,
            hip               = hip,
            bicepLeftRelaxed  = bicepLeftRelaxed,
            bicepLeftFlexed   = bicepLeftFlexed,
            bicepRightRelaxed = bicepRightRelaxed,
            bicepRightFlexed  = bicepRightFlexed,
            forearmLeft       = forearmLeft,
            forearmRight      = forearmRight,
            thighLeft         = thighLeft,
            thighRight        = thighRight,
            calfLeft          = calfLeft,
            calfRight         = calfRight
        )
        Timber.tag(TAG).d("Measurements updated")
    }

    fun updateImprovements(improvements: List<Improvement>) {
        _data.value = _data.value.copy(improvements = improvements)
        Timber.tag(TAG).d("Improvements updated → ${improvements.map { it.name }}")
    }

    fun addPhoto(uri: Uri) {
        _data.value = _data.value.copy(photoUris = _data.value.photoUris + uri)
    }

    fun removePhoto(uri: Uri) {
        _data.value = _data.value.copy(photoUris = _data.value.photoUris - uri)
    }

    // ─── Final save — batch everything to Room ────────────────────────────────────

    /**
     * Called on the final Photos step.
     * Saves everything to Room in one coroutine:
     *   1. Player record
     *   2. Body composition (initialData = true)
     *   3. Body measurements (initialData = true)
     * Photos are uploaded to Firebase Storage immediately (can't be batched).
     * All Room records have isSynced=false — NightlySyncWorker handles Firestore.
     */
    fun saveAll(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            _saveState.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = Resource.Loading()
            Timber.tag(TAG).d("saveAll() → saving onboarding data for uid: $uid")

            try {
                val d = _data.value

                // ── 1. Save Player ────────────────────────────────────────────────
                val playerEntity = PlayerEntity(
                    uid = uid,
                    displayName = d.displayName,
                    birthDate = d.birthDate,
                    gender = d.gender,
                    height = d.height,
                    improvements = d.improvements,
                    needsSync = true   // will be synced by NightlySyncWorker
                )
                playerDao.insertPLayer(playerEntity)
                Timber.tag(TAG).d("✔ Player saved to Room")

                // ── 2. Save Body Composition ──────────────────────────────────────
                val compositionId = UUID.randomUUID().toString()
                val composition   = BodyComposition(
                    id = compositionId,
                    uID = uid,
                    date = Date(),
                    weight = d.weight,
                    bmi = d.bmi,
                    bodyFatPercentage = d.bodyFatPercentage,
                    muscleMassPercentage = d.muscleMassPercentage,
                    visceralFat = d.visceralFat,
                    bodyAge = d.bodyAge,
                    initialData = true,
                    unitSystem = d.unitSystem,
                    photos = emptyList(), // filled after photo upload
                    isSynced = false
                )
                bodyDataRepository.saveComposition(composition)
                Timber.tag(TAG).d("✔ Body composition saved to Room")

                // ── 3. Upload photos to Storage (must happen immediately) ─────────
                if (d.photoUris.isNotEmpty()) {
                    Timber.tag(TAG).d("Uploading ${d.photoUris.size} photo(s)...")
                    val photoResult = bodyDataRepository.uploadCompositionPhotos(
                        uID      = uid,
                        recordId = compositionId,
                        uris     = d.photoUris
                    )
                    if (photoResult is Resource.Error) {
                        Timber.tag(TAG).w("Photo upload failed (non-fatal): ${photoResult.message}")
                    }
                }

                // ── 4. Save Body Measurements ─────────────────────────────────────
                val measurement = BodyMeasurement(
                    id = UUID.randomUUID().toString(),
                    uID = uid,
                    date = Date(),
                    neck = d.neck,
                    shoulders = d.shoulders,
                    chest = d.chest,
                    waist = d.waist,
                    umbilical = d.umbilical,
                    hip = d.hip,
                    bicepLeftRelaxed = d.bicepLeftRelaxed,
                    bicepLeftFlexed = d.bicepLeftFlexed,
                    bicepRightRelaxed = d.bicepRightRelaxed,
                    bicepRightFlexed = d.bicepRightFlexed,
                    forearmLeft = d.forearmLeft,
                    forearmRight = d.forearmRight,
                    thighLeft = d.thighLeft,
                    thighRight = d.thighRight,
                    calfLeft = d.calfLeft,
                    calfRight = d.calfRight,
                    initialData = true,
                    unitSystem = d.unitSystem,
                    isSynced = false
                )
                bodyDataRepository.saveMeasurement(measurement)
                Timber.tag(TAG).d("✔ Body measurements saved to Room")

                // ── Mark onboarding as done in SharedPreferences ──────────────────
                prefs.edit {
                    putBoolean(OnboardingPrefs.KEY_ONBOARDING_DONE, true)
                        .putInt(OnboardingPrefs.KEY_CURRENT_STEP, 0)
                }

                Timber.tag(TAG).i("✔ Onboarding complete — all data saved to Room")
                _saveState.value = Resource.Success(Unit)

                withContext(Dispatchers.Main) { onComplete() }

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "saveAll() failed")
                _saveState.value = Resource.Error("Save failed")
            }
        }
    }
}