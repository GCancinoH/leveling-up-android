package com.gcancino.levelingup.core

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import com.gcancino.levelingup.data.local.database.AppDatabase
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.data.repositories.AuthRepositoryImpl
import com.gcancino.levelingup.data.repositories.BodyCompositionRepositoryImpl
import com.gcancino.levelingup.data.repositories.QuestRepositoryImpl
import com.gcancino.levelingup.presentation.auth.signIn.SignInViewModel
import com.gcancino.levelingup.presentation.auth.signUp.SignUpViewModel
import com.gcancino.levelingup.presentation.initialization.InitViewModel
import com.gcancino.levelingup.presentation.player.dashboard.DashboardViewModel
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.BodyCompositionBottomSheetViewModel
import com.gcancino.levelingup.presentation.player.dashboard.viewModels.QuestMenuViewModel
import com.gcancino.levelingup.ui.components.quests.QuestStartedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlin.getValue

class Container(
    private val context: Context,
    private val application: Application
) {
    val auth by lazy { FirebaseAuth.getInstance() }
    val db by lazy { FirebaseFirestore.getInstance() }
    val storage by lazy { FirebaseStorage.getInstance() }
    val dataStore by lazy { DataStoreManager(context) }

    private val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context.applicationContext)
    }

    // DAOs
    val playerDB by lazy { appDatabase.playerDao() }
    val progressDB by lazy { appDatabase.playerProgressDao() }
    val attributes by lazy { appDatabase.playerAttributesDao() }
    val streakDB by lazy { appDatabase.playerStreakDao() }
    val questDB by lazy { appDatabase.questDao() }
    val bodyCompositionDB by lazy { appDatabase.bodyCompositionDao() }

    // Repositories
    val authRepository by lazy { AuthRepositoryImpl(auth, db, appDatabase) }
    val bodyCompositionRepository by lazy { BodyCompositionRepositoryImpl(bodyCompositionDB, db, storage, auth) }
    val questRepository by lazy { QuestRepositoryImpl(db, questDB, playerDB) }


    // ViewModels
    val initViewModel by lazy { InitViewModel(auth, dataStore, questRepository) }
    val signInViewModel by lazy { SignInViewModel(authRepository) }
    val signUpViewModel by lazy { SignUpViewModel(authRepository, auth, bodyCompositionRepository, playerDB  ) }
    val dashboardViewModel by lazy { DashboardViewModel(questRepository, bodyCompositionRepository) }
    val questMenuViewModel by lazy { QuestMenuViewModel(questRepository) }
    val questStartedViewModel by lazy { QuestStartedViewModel(questRepository, application) }
    val bodyCompositionBottomSheetViewModel by lazy { BodyCompositionBottomSheetViewModel() }
}