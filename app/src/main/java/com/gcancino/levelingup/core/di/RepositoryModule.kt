package com.gcancino.levelingup.core.di

import com.gcancino.levelingup.data.local.database.AppDatabase
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.database.dao.QuestDao
import com.gcancino.levelingup.data.repositories.AuthRepositoryImpl
import com.gcancino.levelingup.data.repositories.BodyCompositionRepositoryImpl
import com.gcancino.levelingup.data.repositories.QuestRepositoryImpl
import com.gcancino.levelingup.domain.repositories.AuthRepository
import com.gcancino.levelingup.domain.repositories.BodyCompositionRepository
import com.gcancino.levelingup.domain.repositories.QuestRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        db: FirebaseFirestore,
        localDB: AppDatabase
    ): AuthRepository = AuthRepositoryImpl(auth, db, localDB)

    @Provides
    @Singleton
    fun provideBodyCompositionRepository(
        bodyCompositionDao: BodyCompositionDao,
        db: FirebaseFirestore,
        storageDB: FirebaseStorage,
        auth: FirebaseAuth
    ): BodyCompositionRepository = BodyCompositionRepositoryImpl(bodyCompositionDao, db, storageDB, auth)

    @Provides
    @Singleton
    fun provideQuestRepository(
        db: FirebaseFirestore,
        questDao: QuestDao,
        playerDao: PlayerDao
    ): QuestRepository = QuestRepositoryImpl(db, questDao, playerDao)
}