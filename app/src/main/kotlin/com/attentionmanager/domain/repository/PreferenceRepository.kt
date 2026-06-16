package com.attentionmanager.domain.repository

import com.attentionmanager.data.preferences.FilterPreferences
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    val preferences: Flow<FilterPreferences>
    val isFilterEnabled: Flow<Boolean>
    suspend fun setFilterEnabled(enabled: Boolean)
    suspend fun setDigestIntervalHours(hours: Int)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setNotificationPermissionGranted(granted: Boolean)
}
