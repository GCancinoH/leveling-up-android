package com.gcancino.levelingup.presentation.auth.signUp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.repositories.AuthRepositoryImpl
import com.gcancino.levelingup.data.repositories.BodyCompositionRepositoryImpl
import com.gcancino.levelingup.presentation.auth.signUp.steps.AccountStepViewModel
import com.gcancino.levelingup.presentation.auth.signUp.steps.BodyCompositionViewModel
import com.gcancino.levelingup.presentation.auth.signUp.steps.ImprovementsViewModel
import com.gcancino.levelingup.presentation.auth.signUp.steps.PersonalInfoViewModel
import com.gcancino.levelingup.presentation.auth.signUp.steps.PhotosViewModel
import com.gcancino.levelingup.presentation.auth.signUp.steps.PhysicalAttributesViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpViewModel(
    private val authRepository: AuthRepositoryImpl,
    private val auth: FirebaseAuth,
    private val bodyCompositionRepository: BodyCompositionRepositoryImpl,
    private val playerDao: PlayerDao
) : ViewModel() {

    val db = FirebaseFirestore.getInstance()

    val accountViewModel by lazy { AccountStepViewModel(this, authRepository) }
    val personalInfoViewModel by lazy { PersonalInfoViewModel(this, authRepository) }
    val physicalAttributesViewModel by lazy { PhysicalAttributesViewModel(this, playerDao, authRepository) }
    val bodyCompositionViewModel by lazy { BodyCompositionViewModel(this, auth, bodyCompositionRepository) }
    val improvementsViewModel by lazy { ImprovementsViewModel(this, authRepository) }
    val photosViewModel by lazy { PhotosViewModel(this, bodyCompositionRepository) }


    var progress by mutableFloatStateOf(0.2f)
    val progressIncrement = 0.2f
    var currentStep by mutableIntStateOf(0)

    fun nextStep() {
        currentStep++
        progress += progressIncrement
    }

    fun previousStep() {
        currentStep--
        progress -= progressIncrement
    }



}