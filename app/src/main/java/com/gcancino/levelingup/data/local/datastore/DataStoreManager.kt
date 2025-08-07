package com.gcancino.levelingup.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gcancino.levelingup.domain.models.dataStore.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DataStoreManager(
    context: Context
) {
    private val dataStore = context.applicationContext.userPreferencesStore

    private object PreferencesKey {
        val IS_PLAYER_DATA_SAVED_LOCALLY = booleanPreferencesKey("is_player_data_saved_locally")
        val ARE_QUESTS_LOADED = booleanPreferencesKey("are_quests_loaded")
        val LAST_QUEST_RESET = stringPreferencesKey("last_quest_reset")
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
                } ?: today
            )
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