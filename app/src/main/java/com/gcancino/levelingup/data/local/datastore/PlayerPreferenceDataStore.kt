package com.gcancino.levelingup.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate

val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "player_preferences"
)