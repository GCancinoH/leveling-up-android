package com.gcancino.levelingup.core.di

import android.content.Context
import com.gcancino.levelingup.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePlayerDao(appDatabase: AppDatabase) = appDatabase.playerDao()

    @Provides
    @Singleton
    fun providePlayerProgressDao(appDatabase: AppDatabase) = appDatabase.playerProgressDao()

    @Provides
    @Singleton
    fun providePlayerAttributesDao(appDatabase: AppDatabase) = appDatabase.playerAttributesDao()

    @Provides
    @Singleton
    fun providePlayerStreakDao(appDatabase: AppDatabase) = appDatabase.playerStreakDao()

    @Provides
    @Singleton
    fun provideQuestDao(appDatabase: AppDatabase) = appDatabase.questDao()

    @Provides
    @Singleton
    fun provideBodyCompositionDao(appDatabase: AppDatabase) = appDatabase.bodyCompositionDao()
}