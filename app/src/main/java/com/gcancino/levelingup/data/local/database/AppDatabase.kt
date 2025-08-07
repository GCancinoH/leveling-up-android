package com.gcancino.levelingup.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gcancino.levelingup.data.local.database.converters.PlayerConverters
import com.gcancino.levelingup.data.local.database.converters.QuestConverters
import com.gcancino.levelingup.data.local.database.dao.BodyCompositionDao
import com.gcancino.levelingup.data.local.database.dao.PlayerAttributesDao
import com.gcancino.levelingup.data.local.database.dao.PlayerDao
import com.gcancino.levelingup.data.local.database.dao.PlayerProgressDao
import com.gcancino.levelingup.data.local.database.dao.PlayerStreakDao
import com.gcancino.levelingup.data.local.database.dao.QuestDao
import com.gcancino.levelingup.data.local.database.entities.BodyCompositionEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerAttributesEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerProgressEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerStreakEntity
import com.gcancino.levelingup.data.local.database.entities.QuestEntity

@Database(
    entities = [
        PlayerEntity::class, PlayerAttributesEntity::class, PlayerProgressEntity::class, PlayerStreakEntity::class,
        BodyCompositionEntity::class, QuestEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(PlayerConverters::class, QuestConverters::class)
abstract class AppDatabase : RoomDatabase() {
    /*abstract fun bodyCompositionDao(): BodyCompositionDao
    abstract fun bloodPressureDao(): BloodPressureDao
    abstract fun questDao(): DailyQuestDao
    abstract fun exerciseProgressDao(): ExerciseProgressDao*/
    abstract fun playerDao(): PlayerDao
    abstract fun playerProgressDao(): PlayerProgressDao
    abstract fun playerStreakDao(): PlayerStreakDao
    abstract fun playerAttributesDao(): PlayerAttributesDao
    abstract fun bodyCompositionDao(): BodyCompositionDao
    abstract fun questDao(): QuestDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "app_database"
                            ).fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}