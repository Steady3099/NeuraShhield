package com.attentionmanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

val Context.filterPreferencesDataStore: DataStore<FilterPreferences> by dataStore(
    fileName = "filter_preferences.pb",
    serializer = FilterPreferencesSerializer
)
