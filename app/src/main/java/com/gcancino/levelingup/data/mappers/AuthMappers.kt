package com.gcancino.levelingup.data.mappers

import com.gcancino.levelingup.data.local.database.entities.PlayerAttributesEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerProgressEntity
import com.gcancino.levelingup.data.local.database.entities.PlayerStreakEntity
import com.gcancino.levelingup.domain.models.bodyComposition.BodyComposition
import com.gcancino.levelingup.domain.models.Player
import com.gcancino.levelingup.domain.models.player.Attributes
import com.gcancino.levelingup.domain.models.player.CategoryType
import com.gcancino.levelingup.domain.models.player.Progress
import com.gcancino.levelingup.domain.models.player.Streak
import java.util.Date


// Domain Player to Room PlayerEntity
fun Player.toEntity(): PlayerEntity {
    return PlayerEntity(
        uid = this.uid,
        displayName = this.displayName,
        email = this.email,
        photoURL = this.photoURL,
        birthDate = this.birthDate,
        age = this.age,
        height = this.height,
        gender = this.gender,
        improvements = this.improvements ?: emptyList(),
        lastSync = Date(),
        needsSync = false
    )
}

// Room PlayerEntity to Domain Player (you'd use this when fetching from DB)
fun PlayerEntity.toDomain(): Player {
    return Player(
        uid = this.uid,
        displayName = this.displayName,
        email = this.email,
        photoURL = this.photoURL,
        birthDate = this.birthDate,
        age = this.age,
        height = this.height,
        gender = this.gender,
        improvements = this.improvements
    )
}

fun PlayerProgressEntity.toDomain(): Progress {
    return Progress(
        uid = this.uid,
        coins = this.coins,
        exp = this.exp,
        level = this.level,
        availablePoints = this.availablePoints,
        currentCategory = CategoryType.fromString(this.currentCategory)
    )
}

fun Progress.toEntity(): PlayerProgressEntity {
    return PlayerProgressEntity(
        uid = this.uid,
        coins = this.coins,
        exp = this.exp,
        level = this.level,
        availablePoints = this.availablePoints,
        currentCategory = this.currentCategory.name
    )
}

fun PlayerAttributesEntity.toDomain(): Attributes {
    return Attributes(
        uid = this.uid,
        strength = this.strength,
        endurance = this.endurance,
        intelligence = this.intelligence,
        mobility = this.mobility,
        health = this.health,
        finance = this.finance
    )
}

fun Attributes.toEntity() : PlayerAttributesEntity {
    return PlayerAttributesEntity(
        uid = this.uid,
        strength = this.strength,
        endurance = this.endurance,
        intelligence = this.intelligence,
        mobility = this.mobility,
        health = this.health,
        finance = this.finance
    )
}

fun PlayerStreakEntity.toDomain(): Streak {
    return Streak(
        uid = this.uid,
        currentStreak = this.currentStreak,
        longestStreak = this.longestStreak,
        lastStreakUpdate = this.lastStreakUpdate?.let { Date(it) } ?: Date()
    )
}

fun Streak.toEntity(): PlayerStreakEntity {
    return PlayerStreakEntity(
        uid = this.uid,
        currentStreak = this.currentStreak,
        longestStreak = this.longestStreak,
        lastStreakUpdate = this.lastStreakUpdate?.time
    )

}

fun PlayerEntity.isFresh(maxAgeMinutes: Long = 480): Boolean {
    val currentTime = System.currentTimeMillis()
    val lastSyncTime = lastSync.time
    val ageInMinutes = (currentTime - lastSyncTime) / (1000 * 60)
    return ageInMinutes <= maxAgeMinutes
}
