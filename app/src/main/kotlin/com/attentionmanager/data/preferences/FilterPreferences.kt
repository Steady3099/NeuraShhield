package com.attentionmanager.data.preferences

data class FilterPreferences(
    val aiFilterEnabled: Boolean = true,
    val digestMinutesOfDayList: List<Int> = emptyList(),
    val onboardingCompleted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val digestIntervalHours: Int = 1
) {
    companion object {
        fun getDefaultInstance(): FilterPreferences = FilterPreferences()
    }
}
