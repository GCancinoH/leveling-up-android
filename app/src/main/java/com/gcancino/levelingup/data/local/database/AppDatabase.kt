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
        GeneratedQuestEntity::class
    ],
    version = 7,
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
    abstract fun bodyMeasurementsDao(): BodyMeasurementDao
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun morningEntryDao(): MorningEntryDao
    abstract fun eveningEntryDao(): EveningEntryDao
    abstract fun penaltyEventDao(): PenaltyEventDao
    abstract fun identityProfileDao(): IdentityProfileDao
    abstract fun dailyStandardEntryDao(): DailyStandardEntryDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun generatedQuestDao(): GeneratedQuestDao


    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS player_progress_new (
                id INTEGER PRIMARY KEY NOT NULL,
                uid TEXT NOT NULL,
                level INTEGER,
                exp INTEGER,
                coins INTEGER,
                availablePoints INTEGER NOT NULL,
                currentCategory TEXT,
                lastLevelUpdate INTEGER,
                lastCategoryUpdate INTEGER,
                needSync INTEGER NOT NULL DEFAULT 0,  -- ← no quotes
                lastSync INTEGER
            )
        """.trimIndent()
                )

                db.execSQL(
                    """
            INSERT INTO player_progress_new (
                id, uid, level, exp, coins, availablePoints,
                currentCategory, lastLevelUpdate, lastCategoryUpdate, needSync
            )
            SELECT id, uid, level, exp, coins, availablePoints,
                   currentCategory, lastLevelUpdate, lastCategoryUpdate, 0
            FROM player_progress
        """.trimIndent()
                )

                db.execSQL("DROP TABLE player_progress")
                db.execSQL("ALTER TABLE player_progress_new RENAME TO player_progress")

                try {
                    db.execSQL(
                        "ALTER TABLE player_attributes ADD COLUMN needSync INTEGER NOT NULL DEFAULT 0"  // ← no quotes
                    )
                    db.execSQL(
                        "ALTER TABLE player_attributes ADD COLUMN lastSync INTEGER"
                    )
                } catch (e: Exception) {
                }
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
                db.execSQL(
                    """
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
                """.trimIndent()
                )

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
                } catch (e: Exception) {
                }
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create table WITHOUT 'DEFAULT' keywords to match the Entity
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS daily_standard_entries (
                id TEXT PRIMARY KEY NOT NULL,
                uID TEXT NOT NULL,
                standardId TEXT NOT NULL,
                standardTitle TEXT NOT NULL,
                standardType TEXT NOT NULL,
                roleId TEXT NOT NULL,
                roleName TEXT NOT NULL,
                date INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL,
                completedAt INTEGER,
                xpAwarded INTEGER NOT NULL,
                autoValidated INTEGER NOT NULL,
                penaltyApplied INTEGER NOT NULL,
                isSynced INTEGER NOT NULL
            )
        """.trimIndent()
                )

                // 2. ONLY keep these if you add 'indices = [...]' to your @Entity class.
                // If you don't want to modify the Entity, DELETE these next two lines:
                // db.execSQL("CREATE INDEX IF NOT EXISTS idx_dse_uid_date ON daily_standard_entries (uID, date)")
                // db.execSQL("CREATE INDEX IF NOT EXISTS idx_dse_uid_role ON daily_standard_entries (uID, roleId)")

                // 3. identity_profile table...
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS identity_profile (
                id TEXT PRIMARY KEY NOT NULL,
                uID TEXT NOT NULL,
                identityStatement TEXT NOT NULL,
                roles TEXT NOT NULL,
                standards TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                isSynced INTEGER NOT NULL
            )
        """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_reports` (
                        `id` TEXT NOT NULL,
                        `uID` TEXT NOT NULL,
                        `weekStart` INTEGER NOT NULL,
                        `overallScore` REAL NOT NULL,
                        `headline` TEXT NOT NULL,
                        `strongestRole` TEXT NOT NULL,
                        `weakestRole` TEXT NOT NULL,
                        `patternIdentified` TEXT NOT NULL,
                        `mirrorInsight` TEXT NOT NULL,
                        `oneCorrection` TEXT NOT NULL,
                        `identityAlignment` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `generated_quests` (
                        `id` TEXT NOT NULL,
                        `uID` TEXT NOT NULL,
                        `weeklyReportId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `targetStandardIds` TEXT NOT NULL,
                        `goal` INTEGER NOT NULL,
                        `durationDays` INTEGER NOT NULL,
                        `currentProgress` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `endDate` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `xpReward` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )
            }
        }
    }
}