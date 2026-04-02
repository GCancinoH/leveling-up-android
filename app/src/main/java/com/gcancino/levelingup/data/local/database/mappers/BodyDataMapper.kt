package com.gcancino.levelingup.data.local.database.mappers

import com.gcancino.levelingup.data.local.database.entities.bodyData.BodyCompositionEntity
import com.gcancino.levelingup.data.local.database.entities.bodyData.BodyMeasurementEntity
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.bodyComposition.BodyMeasurement
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem

fun BodyCompositionEntity.toDomain() = BodyComposition(
    id = id,
    uID = uID,
    date = date,
    weight = weight,
    bmi = bmi,
    bodyFatPercentage = bodyFatPercentage,
    muscleMassPercentage = muscleMassPercentage,
    visceralFat = visceralFat,
    bodyAge = bodyAge,
    initialData = initialData,
    photos = photos,
    unitSystem = UnitSystem.valueOf(unitSystem),
    isSynced = isSynced
)

fun BodyComposition.toEntity() = BodyCompositionEntity(
    id = id,
    uID = uID,
    date = date,
    weight = weight,
    bmi = bmi,
    bodyFatPercentage = bodyFatPercentage,
    muscleMassPercentage = muscleMassPercentage,
    visceralFat = visceralFat,
    bodyAge = bodyAge,
    initialData = initialData,
    unitSystem = unitSystem.name,
    photos = photos,
    isSynced = isSynced
)

fun BodyMeasurementEntity.toDomain() = BodyMeasurement(
    id = id,
    uID = uID,
    date = date,
    neck = neck,
    shoulders = shoulders,
    chest = chest,
    waist = waist,
    umbilical = umbilical,
    hip = hip,
    bicepLeftRelaxed = bicepLeftRelaxed,
    bicepLeftFlexed = bicepLeftFlexed,
    bicepRightRelaxed = bicepRightRelaxed,
    bicepRightFlexed = bicepRightFlexed,
    forearmLeft = forearmLeft,
    forearmRight = forearmRight,
    thighLeft = thighLeft,
    thighRight = thighRight,
    calfLeft = calfLeft,
    calfRight = calfRight,
    initialData = initialData,
    unitSystem = UnitSystem.valueOf(unitSystem),
    isSynced = isSynced
)

fun BodyMeasurement.toEntity() = BodyMeasurementEntity(
    id = id,
    uID = uID,
    date = date,
    neck = neck,
    shoulders = shoulders,
    chest = chest,
    waist = waist,
    umbilical = umbilical,
    hip = hip,
    bicepLeftRelaxed = bicepLeftRelaxed,
    bicepLeftFlexed = bicepLeftFlexed,
    bicepRightRelaxed = bicepRightRelaxed,
    bicepRightFlexed = bicepRightFlexed,
    forearmLeft = forearmLeft,
    forearmRight = forearmRight,
    thighLeft = thighLeft,
    thighRight = thighRight,
    calfLeft = calfLeft,
    calfRight = calfRight,
    initialData = initialData,
    unitSystem = unitSystem.name,
    isSynced = isSynced
)