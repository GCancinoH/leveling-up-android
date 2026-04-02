package com.gcancino.levelingup.data.local.database.converters

import androidx.room.TypeConverter
import com.gcancino.levelingup.domain.models.exercise.BlockType
import com.gcancino.levelingup.domain.models.exercise.ExerciseRole
import com.gcancino.levelingup.domain.models.exercise.IntensityType

class ExerciseConverters {
    @TypeConverter
    fun fromBlockType(value: BlockType): String {
        return value.name
    }

    @TypeConverter
    fun toBlockType(value: String): BlockType {
        return BlockType.valueOf(value)
    }

    @TypeConverter
    fun fromIntensityType(value: IntensityType): String {
        return value.name
    }

    @TypeConverter
    fun toIntensityType(value: String): IntensityType {
        return IntensityType.valueOf(value)
    }

    @TypeConverter
    fun fromExerciseRole(value: ExerciseRole): String {
        return value.name
    }

    @TypeConverter
    fun toExerciseRole(value: String): ExerciseRole {
        return ExerciseRole.valueOf(value)
    }
}
