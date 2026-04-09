package com.gcancino.levelingup.core.di

import android.content.Context
import androidx.room.Room
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "leveling_up_db"
        ).addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration(false) // Added to handle schema changes during development
        .build()
    }

    @Provides
    @Singleton
    fun providePlayerDao(db: AppDatabase) = db.playerDao()

    @Provides
    @Singleton
    fun providePlayerAttributesDao(db: AppDatabase) = db.playerAttributesDao()

    @Provides
    @Singleton
    fun providePlayerProgressDao(db: AppDatabase) = db.playerProgressDao()

    @Provides
    @Singleton
    fun providePlayerStreakDao(db: AppDatabase) = db.playerStreakDao()

    @Provides
    @Singleton
    fun provideBodyCompositionDao(db: AppDatabase) = db.bodyCompositionDao()

    @Provides
    @Singleton
    fun provideBodyMeasurementDao(db: AppDatabase) = db.bodyMeasurementsDao()

    @Provides
    @Singleton
    fun provideQuestDao(db: AppDatabase) = db.questDao()

    @Provides
    @Singleton
    fun provideExerciseDao(db: AppDatabase) = db.exerciseDao()

    @Provides
    @Singleton
    fun provideDailyTaskDao(db: AppDatabase) = db.dailyTaskDao()

    @Provides
    @Singleton
    fun provideMorningEntryDao(db: AppDatabase) = db.morningEntryDao()

    @Provides
    @Singleton
    fun provideEveningEntryDao(db: AppDatabase) = db.eveningEntryDao()

    @Provides
    @Singleton
    fun providePenaltyEventDao(db: AppDatabase) = db.penaltyEventDao()

    @Provides
    @Singleton
    fun provideIdentityProfileDao(db: AppDatabase) = db.identityProfileDao()

    @Provides
    @Singleton
    fun provideDailyStandardEntryDao(db: AppDatabase) = db.dailyStandardEntryDao()

    @Provides
    @Singleton
    fun provideWeeklyReportDao(db: AppDatabase) = db.weeklyReportDao()

    @Provides
    @Singleton
    fun provideGeneratedQuestDao(db: AppDatabase) = db.generatedQuestDao()
}
