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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class DataStoreManager(
    context: Context,
    private val encryptedDraftsManager: EncryptedDraftsManager? = null,
    private val dataStoreCryptoManager: DataStoreCryptoManager? = null
) {
    private val dataStore = context.applicationContext.userPreferencesStore

    private object PreferencesKey {
        val IS_PLAYER_DATA_SAVED_LOCALLY = booleanPreferencesKey("is_player_data_saved_locally")
        val ARE_QUESTS_LOADED = booleanPreferencesKey("are_quests_loaded")
        val LAST_QUEST_RESET = stringPreferencesKey("last_quest_reset_enc")
        val LAST_PENALTY_XP_LOST = stringPreferencesKey("last_penalty_xp_lost_enc")
        val LAST_PENALTY_STREAK_LOST = stringPreferencesKey("last_penalty_streak_lost_enc")
        val LAST_PENALTY_DATE = stringPreferencesKey("last_penalty_date_enc")
        val LAST_PENALTY_TASKS_COUNT = stringPreferencesKey("last_penalty_tasks_count_enc")

        // Daily Reset
        val LAST_EVALUATED_DATE = stringPreferencesKey("last_evaluated_date_enc")
        val CONSECUTIVE_FAILURES = stringPreferencesKey("consecutive_failures_enc")
        
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
            val questResetRaw = preferences[PreferencesKey.LAST_QUEST_RESET]
            val questResetStr = questResetRaw?.let { dataStoreCryptoManager?.decrypt(it) ?: it }

            val penaltyDateRaw = preferences[PreferencesKey.LAST_PENALTY_DATE]
            val penaltyDateStr = penaltyDateRaw?.let { dataStoreCryptoManager?.decrypt(it) ?: it }

            PlayerPreferences(
                isPlayerDataSavedLocally = preferences[PreferencesKey.IS_PLAYER_DATA_SAVED_LOCALLY] ?: false,
                areQuestsLoaded = preferences[PreferencesKey.ARE_QUESTS_LOADED] ?: false,
                lastQuestReset = questResetStr?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        today
                    }
                } ?: today,
                lastPenaltyXpLost = preferences[PreferencesKey.LAST_PENALTY_XP_LOST]?.let { 
                    dataStoreCryptoManager?.decryptInt(it) ?: it.toIntOrNull() 
                } ?: 0,
                lastPenaltyStreakLost = preferences[PreferencesKey.LAST_PENALTY_STREAK_LOST]?.let { 
                    dataStoreCryptoManager?.decryptInt(it) ?: it.toIntOrNull() 
                } ?: 0,
                lastPenaltyDate = penaltyDateStr?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        today
                    }
                } ?: today,
                lastPenaltyTasksCount = preferences[PreferencesKey.LAST_PENALTY_TASKS_COUNT]?.let { 
                    dataStoreCryptoManager?.decryptInt(it) ?: it.toIntOrNull() 
                } ?: 0
            )
        }

    // --- Drafts Logic (using EncryptedDraftsManager) ---
    
    suspend fun saveMorningDraft(answers: Map<String, String>) {
        encryptedDraftsManager?.saveMorningDraft(answers)
            ?: run {
                dataStore.edit { preferences ->
                    preferences[PreferencesKey.MORNING_DRAFT] = Json.encodeToString(answers)
                }
            }
    }

    suspend fun getMorningDraft(): Map<String, String> {
        return encryptedDraftsManager?.getMorningDraft()
            ?: run {
                val preferences = dataStore.data.firstOrNull() ?: return emptyMap()
                val json = preferences[PreferencesKey.MORNING_DRAFT] ?: return emptyMap()
                try {
                    Json.decodeFromString(json)
                } catch (e: Exception) {
                    emptyMap()
                }
            }
    }

    suspend fun clearMorningDraft() {
        encryptedDraftsManager?.clearMorningDraft()
            ?: run {
                dataStore.edit { preferences ->
                    preferences.remove(PreferencesKey.MORNING_DRAFT)
                }
            }
    }

    suspend fun saveEveningDraft(answers: Map<String, String>) {
        encryptedDraftsManager?.saveEveningDraft(answers)
            ?: run {
                dataStore.edit { preferences ->
                    preferences[PreferencesKey.EVENING_DRAFT] = Json.encodeToString(answers)
                }
            }
    }

    suspend fun getEveningDraft(): Map<String, String> {
        return encryptedDraftsManager?.getEveningDraft()
            ?: run {
                val preferences = dataStore.data.firstOrNull() ?: return emptyMap()
                val json = preferences[PreferencesKey.EVENING_DRAFT] ?: return emptyMap()
                try {
                    Json.decodeFromString(json)
                } catch (e: Exception) {
                    emptyMap()
                }
            }
    }

    suspend fun clearEveningDraft() {
        encryptedDraftsManager?.clearEveningDraft()
            ?: run {
                dataStore.edit { preferences ->
                    preferences.remove(PreferencesKey.EVENING_DRAFT)
                }
            }
    }

    // --- Daily Reset Logic ---

    suspend fun getLastEvaluatedDate(): String? {
        val preferences = dataStore.data.firstOrNull() ?: return null
        val raw = preferences[PreferencesKey.LAST_EVALUATED_DATE] ?: return null
        return dataStoreCryptoManager?.decrypt(raw) ?: raw
    }

    suspend fun setLastEvaluatedDate(date: String) {
        val encrypted = dataStoreCryptoManager?.encrypt(date) ?: date
        dataStore.edit { preferences ->
            preferences[PreferencesKey.LAST_EVALUATED_DATE] = encrypted
        }
    }

    suspend fun getConsecutiveFailures(): Int {
        val preferences = dataStore.data.firstOrNull() ?: return 0
        val raw = preferences[PreferencesKey.CONSECUTIVE_FAILURES] ?: return 0
        return dataStoreCryptoManager?.decryptInt(raw) ?: raw.toIntOrNull() ?: 0
    }

    suspend fun saveConsecutiveFailures(count: Int) {
        val encrypted = dataStoreCryptoManager?.encryptInt(count) ?: count.toString()
        dataStore.edit { preferences ->
            preferences[PreferencesKey.CONSECUTIVE_FAILURES] = encrypted
        }
    }

    // --- Penalty Logic ---
    
    suspend fun savePenalty(summary: PenaltySummary) {
        val encXp = dataStoreCryptoManager?.encryptInt(summary.xpLost) ?: summary.xpLost.toString()
        val encStreak = dataStoreCryptoManager?.encryptInt(summary.streakLost) ?: summary.streakLost.toString()
        val encTasks = dataStoreCryptoManager?.encryptInt(summary.incompleteTasks) ?: summary.incompleteTasks.toString()
        val encDate = dataStoreCryptoManager?.encrypt(today.toString()) ?: today.toString()

        dataStore.edit { preferences ->
            preferences[PreferencesKey.LAST_PENALTY_XP_LOST] = encXp
            preferences[PreferencesKey.LAST_PENALTY_STREAK_LOST] = encStreak
            preferences[PreferencesKey.LAST_PENALTY_TASKS_COUNT] = encTasks
            preferences[PreferencesKey.LAST_PENALTY_DATE] = encDate
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
            val encDate = dataStoreCryptoManager?.encrypt(date.toString()) ?: date.toString()
            dataStore.edit { preferences ->
                preferences[PreferencesKey.ARE_QUESTS_LOADED] = true
                preferences[PreferencesKey.LAST_QUEST_RESET] = encDate
            }
        } catch (e: Exception) {
            // Handle edit failure
        }
    }

    suspend fun needsQuestRefresh(): Boolean {
        return userPreferences.firstOrNull()?.let { prefs ->
            !prefs.areQuestsLoaded || prefs.lastQuestReset.isBefore(today)
        } ?: true
    }
}
