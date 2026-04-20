package com.gcancino.levelingup.data.local.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import androidx.core.content.edit

class EncryptedDraftsManager(context: Context) {

    private val TAG = "EncryptedDraftsManager"

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "encrypted_drafts_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_MORNING_DRAFT = "morning_draft"
        private const val KEY_EVENING_DRAFT = "evening_draft"
    }

    suspend fun saveMorningDraft(answers: Map<String, String>) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(answers)
                encryptedPrefs.edit { putString(KEY_MORNING_DRAFT, jsonString) }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save morning draft")
            }
        }
    }

    suspend fun getMorningDraft(): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = encryptedPrefs.getString(KEY_MORNING_DRAFT, null)
                if (jsonString != null) {
                    json.decodeFromString<Map<String, String>>(jsonString)
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to get morning draft")
                emptyMap()
            }
        }
    }

    suspend fun clearMorningDraft() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit().remove(KEY_MORNING_DRAFT).apply()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to clear morning draft")
            }
        }
    }

    suspend fun saveEveningDraft(answers: Map<String, String>) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(answers)
                encryptedPrefs.edit { putString(KEY_EVENING_DRAFT, jsonString) }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save evening draft")
            }
        }
    }

    suspend fun getEveningDraft(): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = encryptedPrefs.getString(KEY_EVENING_DRAFT, null)
                if (jsonString != null) {
                    json.decodeFromString<Map<String, String>>(jsonString)
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to get evening draft")
                emptyMap()
            }
        }
    }

    suspend fun clearEveningDraft() {
        withContext(Dispatchers.IO) {
            try {
                encryptedPrefs.edit { remove(KEY_EVENING_DRAFT) }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to clear evening draft")
            }
        }
    }
}