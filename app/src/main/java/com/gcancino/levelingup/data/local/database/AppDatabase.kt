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
import com.gcancino.levelingup.data.local.database.entities.*
import com.gcancino.levelingup.data.local.database.entities.bodyData.*
import com.gcancino.levelingup.data.local.database.entities.dailyTasks.*

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
        PenaltyEventEntity::class
    ],
    version = 4,
    exportSchema = false
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
    abstract fun bodyMeasurementsDao() : BodyMeasurementDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun morningEntryDao(): MorningEntryDao
    abstract fun eveningEntryDao(): EveningEntryDao
    abstract fun penaltyEventDao(): PenaltyEventDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Update training_sessions
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS training_sessions_new (
                        id TEXT PRIMARY KEY NOT NULL, 
                        completed INTEGER NOT NULL, 
                        macrocycleId TEXT, 
                        mesocycleId TEXT, 
                        microcycleId TEXT, 
                        date INTEGER NOT NULL, 
                        name TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO training_sessions_new (id, completed, microcycleId, date, name)
                    SELECT id, completed, microcycleId, date, name FROM training_sessions
                """.trimIndent())

                db.execSQL("DROP TABLE training_sessions")
                db.execSQL("ALTER TABLE training_sessions_new RENAME TO training_sessions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_training_sessions_microcycleId ON training_sessions (microcycleId)")

                // 2. Add columns to Player Progress
                try {
                    // For nullable Boolean?, we can remove DEFAULT 0 to match 'undefined'
                    db.execSQL("ALTER TABLE player_attributes ADD COLUMN needSync INTEGER")
                    db.execSQL("ALTER TABLE player_attributes ADD COLUMN lastSync INTEGER")
                } catch (e: Exception) { }

                // 3. Add columns to Player Attributes
                try {
                    db.execSQL("ALTER TABLE player_attributes ADD COLUMN needSync INTEGER DEFAULT 0")
                    db.execSQL("ALTER TABLE player_attributes ADD COLUMN lastSync INTEGER")
                } catch (e: Exception) { }
                
                // 4. Body tables
                db.execSQL("""CREATE TABLE IF NOT EXISTS body_composition (
                    id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL,
                    weight REAL NOT NULL, bmi REAL NOT NULL, bodyFatPercentage REAL NOT NULL,
                    muscleMassPercentage REAL NOT NULL, visceralFat REAL NOT NULL,
                    bodyAge INTEGER NOT NULL, initialData INTEGER NOT NULL,
                    unitSystem TEXT NOT NULL, isSynced INTEGER NOT NULL DEFAULT 0)""")
                
                db.execSQL("""CREATE TABLE IF NOT EXISTS body_measurements (
                    id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL,
                    neck REAL NOT NULL, shoulders REAL NOT NULL, chest REAL NOT NULL,
                    waist REAL NOT NULL, umbilical REAL NOT NULL, hip REAL NOT NULL,
                    bicepLeftRelaxed REAL NOT NULL, bicepLeftFlexed REAL NOT NULL,
                    bicepRightRelaxed REAL NOT NULL, bicepRightFlexed REAL NOT NULL,
                    forearmLeft REAL NOT NULL, forearmRight REAL NOT NULL,
                    thighLeft REAL NOT NULL, thighRight REAL NOT NULL,
                    calfLeft REAL NOT NULL, calfRight REAL NOT NULL,
                    initialData INTEGER NOT NULL, unitSystem TEXT NOT NULL,
                    isSynced INTEGER NOT NULL DEFAULT 0)""")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // morning_entries
                db.execSQL("CREATE TABLE IF NOT EXISTS morning_entries (id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL, answers TEXT NOT NULL, isSynced INTEGER NOT NULL DEFAULT 0)")

                // evening_entries
                db.execSQL("CREATE TABLE IF NOT EXISTS evening_entries (id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL, answers TEXT NOT NULL, isSynced INTEGER NOT NULL DEFAULT 0)")

                // daily_tasks - Force recreation to ensure correct types (especially 'priority' as TEXT)
                db.execSQL("DROP TABLE IF EXISTS daily_tasks")
                db.execSQL("""
                    CREATE TABLE daily_tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        uID TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        xpReward INTEGER NOT NULL,
                        penaltyApplied INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL
                    )
                """.trimIndent())

                // morning_entries
                db.execSQL("DROP TABLE IF EXISTS morning_entries")
                db.execSQL("CREATE TABLE morning_entries (id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL, answers TEXT NOT NULL, isSynced INTEGER NOT NULL)")

                // evening_entries
                db.execSQL("DROP TABLE IF EXISTS evening_entries")
                db.execSQL("CREATE TABLE evening_entries (id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL, answers TEXT NOT NULL, isSynced INTEGER NOT NULL)")

                // penalty_events
                db.execSQL("DROP TABLE IF EXISTS penalty_events")
                db.execSQL("CREATE TABLE penalty_events (id TEXT PRIMARY KEY NOT NULL, uID TEXT NOT NULL, date INTEGER NOT NULL, xpLost INTEGER NOT NULL, streakLost INTEGER NOT NULL, incompleteTasks TEXT NOT NULL, isSynced INTEGER NOT NULL)")

                // photos column in body_composition
                try {
                    // Note: If data exists, ALTER TABLE requires a DEFAULT.
                    // To match Room's 'undefined', we'd need to recreate this table too if it causes a mismatch.
                    db.execSQL("ALTER TABLE body_composition ADD COLUMN photos TEXT NOT NULL DEFAULT '[]'")
                } catch (e: Exception) { }
            }
        }
    }
}
