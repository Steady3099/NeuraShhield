package com.attentionmanager.domain.usecase

import com.attentionmanager.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AttentionManagerController(
    private val preferences: PreferenceRepository
) {
    val isEnabled: Flow<Boolean> = preferences.isFilterEnabled

    suspend fun enable() = preferences.setFilterEnabled(true)

    suspend fun disable() = preferences.setFilterEnabled(false)

    suspend fun toggle(): Boolean {
        val enabled = !preferences.isFilterEnabled.first()
        preferences.setFilterEnabled(enabled)
        return enabled
    }
}
