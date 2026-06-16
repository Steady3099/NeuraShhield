package com.attentionmanager.domain.model

data class AppContext(
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val hasMeetingNext60Min: Boolean = false,
    val currentHour: Int = 0
)

enum class ActivityType {
    IN_VEHICLE,
    ON_FOOT,
    STILL,
    UNKNOWN
}
