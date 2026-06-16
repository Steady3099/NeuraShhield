package com.attentionmanager.data.repository

import androidx.datastore.core.DataStore
import com.attentionmanager.data.preferences.FilterPreferences
import com.attentionmanager.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferenceRepositoryImpl(
    private val dataStore: DataStore<FilterPreferences>
) : PreferenceRepository {
    override val preferences: Flow<FilterPreferences> = dataStore.data

    override val isFilterEnabled: Flow<Boolean> = preferences.map { it.aiFilterEnabled }

    override suspend fun setFilterEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(aiFilterEnabled = enabled) }
    }

    override suspend fun setDigestIntervalHours(hours: Int) {
        dataStore.updateData { it.copy(digestIntervalHours = hours.coerceIn(1, 24)) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.updateData { it.copy(onboardingCompleted = completed) }
    }

    override suspend fun setNotificationPermissionGranted(granted: Boolean) {
        dataStore.updateData { it.copy(notificationPermissionGranted = granted) }
    }
}
