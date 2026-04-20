package com.gcancino.levelingup.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gcancino.levelingup.data.local.database.converters.*
import com.gcancino.levelingup.data.local.database.dao.*
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.DailyTaskDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.EveningEntryDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.MorningEntryDao
import com.gcancino.levelingup.data.local.database.dao.dailyTasks.PenaltyEventDao
import com.gcancino.levelingup.data.local.database.dao.IdentityProfileDao
import com.gcancino.levelingup.data.local.database.entities.*
import com.gcancino.levelingup.data.local.database.entities.bodyData.*
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.*
import com.gcancino.levelingup.data.local.database.entities.identity.*

@Database(
    entities = [
        QuestEntity::class,
        PlayerEntity::class,
        PlayerProgressEntity::class,
        PlayerAttributesEntity::class,
        PlayerStreakEntity::class,
        BodyCompositionEntity::class,
        BodyMeasurementEntity::class,
        MacrocycleEntity::class,
        MesocycleEntity::class,
        MicrocycleEntity::class,
        TrainingSessionEntity::class,
        ExerciseBlockEntity::class,
        ExerciseEntity::class,
        ExerciseSetEntity::class,
        SetLogEntity::class,
        MorningEntryEntity::class,
        EveningEntryEntity::class,
        DailyTaskEntity::class,
        PenaltyEventEntity::class,
        IdentityProfileEntity::class,
        DailyStandardEntryEntity::class,
        WeeklyReportEntity::class,
        GeneratedQuestEntity::class,
        NutritionEntryEntity::class,
        ObjectiveEntity::class,
        WeeklyEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(
    CommonConverters::class,
    QuestConverters::class,
    PlayerConverters::class,
    ExerciseConverters::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
    abstract fun playerDao(): PlayerDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun playerAttributesDao(): PlayerAttributesDao
    abstract fun playerStreakDao(): PlayerStreakDao
    abstract fun bodyCompositionDao(): BodyCompositionDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun bodyMeasurementsDao(): BodyMeasurementDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun morningEntryDao(): MorningEntryDao
    abstract fun eveningEntryDao(): EveningEntryDao
    abstract fun penaltyEventDao(): PenaltyEventDao
    abstract fun identityProfileDao(): IdentityProfileDao
    abstract fun dailyStandardEntryDao(): DailyStandardEntryDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun generatedQuestDao(): GeneratedQuestDao
    abstract fun nutritionEntryDao(): NutritionEntryDao
    abstract fun objectiveDao(): com.gcancino.levelingup.data.local.database.dao.identity.ObjectiveDao
    abstract fun weeklyEntryDao(): com.gcancino.levelingup.data.local.database.dao.dailyTasks.WeeklyEntryDao


    companion object {
    }
}