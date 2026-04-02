package com.gcancino.levelingup.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gcancino.levelingup.domain.models.dailyTasks.PenaltySummary
import com.gcancino.levelingup.domain.models.dataStore.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class DataStoreManager(
    context: Context
) {
    private val dataStore = context.applicationContext.userPreferencesStore

    private object PreferencesKey {
        val IS_PLAYER_DATA_SAVED_LOCALLY = booleanPreferencesKey("is_player_data_saved_locally")
        val ARE_QUESTS_LOADED = booleanPreferencesKey("are_quests_loaded")
        val LAST_QUEST_RESET = stringPreferencesKey("last_quest_reset")
        val LAST_PENALTY_XP_LOST = intPreferencesKey("last_penalty_xp_lost")
        val LAST_PENALTY_STREAK_LOST = intPreferencesKey("last_penalty_streak_lost")
        val LAST_PENALTY_DATE = stringPreferencesKey("last_penalty_date")
        val LAST_PENALTY_TASKS_COUNT = intPreferencesKey("last_penalty_tasks_count")
        
        // Drafts
        val MORNING_DRAFT = stringPreferencesKey("morning_draft")
        val EVENING_DRAFT = stringPreferencesKey("evening_draft")
    }

    private val today = LocalDate.now()

    val userPreferences: Flow<PlayerPreferences> = dataStore.data
        .catch { exception ->
            emit(emptyPreferences())
        }
        .map { preferences ->
            PlayerPreferences(
                isPlayerDataSavedLocally = preferences[PreferencesKey.IS_PLAYER_DATA_SAVED_LOCALLY] ?: false,
                areQuestsLoaded = preferences[PreferencesKey.ARE_QUESTS_LOADED] ?: false,
                lastQuestReset = preferences[PreferencesKey.LAST_QUEST_RESET]?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        today
                    }
                } ?: today,
                lastPenaltyXpLost = preferences[PreferencesKey.LAST_PENALTY_XP_LOST] ?: 0,
                lastPenaltyStreakLost = preferences[PreferencesKey.LAST_PENALTY_STREAK_LOST] ?: 0,
                lastPenaltyDate = preferences[PreferencesKey.LAST_PENALTY_DATE]?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        today
                    }
                } ?: today,
                lastPenaltyTasksCount = preferences[PreferencesKey.LAST_PENALTY_TASKS_COUNT] ?: 0
            )
        }

    // --- Drafts Logic ---
    
    suspend fun saveMorningDraft(answers: Map<String, String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.MORNING_DRAFT] = Json.encodeToString(answers)
        }
    }

    suspend fun getMorningDraft(): Map<String, String> {
        val preferences = dataStore.data.first()
        val json = preferences[PreferencesKey.MORNING_DRAFT] ?: return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun clearMorningDraft() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKey.MORNING_DRAFT)
        }
    }

    suspend fun saveEveningDraft(answers: Map<String, String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.EVENING_DRAFT] = Json.encodeToString(answers)
        }
    }

    suspend fun getEveningDraft(): Map<String, String> {
        val preferences = dataStore.data.first()
        val json = preferences[PreferencesKey.EVENING_DRAFT] ?: return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun clearEveningDraft() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKey.EVENING_DRAFT)
        }
    }

    // --- Penalty Logic ---
    
    suspend fun savePenalty(summary: PenaltySummary) {
        dataStore.edit { preferences ->
            preferences[PreferencesKey.LAST_PENALTY_XP_LOST] = summary.xpLost
            preferences[PreferencesKey.LAST_PENALTY_STREAK_LOST] = summary.streakLost
            preferences[PreferencesKey.LAST_PENALTY_TASKS_COUNT] = summary.incompleteTasks
            preferences[PreferencesKey.LAST_PENALTY_DATE] = today.toString()
        }
    }
    
    suspend fun clearPenalty() {
        dataStore.edit { preferences -> 
            preferences.remove(PreferencesKey.LAST_PENALTY_XP_LOST)
            preferences.remove(PreferencesKey.LAST_PENALTY_DATE)
        }
    }

    suspend fun updatePlayerDataSavedStatus(isSaved: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKey.IS_PLAYER_DATA_SAVED_LOCALLY] = isSaved
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateQuestLoadedStatus(date: LocalDate = LocalDate.now()) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKey.ARE_QUESTS_LOADED] = true
                preferences[PreferencesKey.LAST_QUEST_RESET] = date.toString()
            }
        } catch (e: Exception) {
            // Handle edit failure
        }
    }

    suspend fun needsQuestRefresh(): Boolean {
        return userPreferences.first().let { prefs ->
            !prefs.areQuestsLoaded || prefs.lastQuestReset.isBefore(today)
        }
    }
}
