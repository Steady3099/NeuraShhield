package com.attentionmanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attentionmanager.domain.model.PriorityTier
import com.attentionmanager.domain.repository.DigestRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.PreferenceRepository
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    notificationRepository: NotificationRepository,
    digestRepository: DigestRepository,
    preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val dayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val uiState = combine(
        notificationRepository.observeQuietMinutesToday(dayStart),
        notificationRepository.observeInterruptionCountSince(dayStart),
        digestRepository.observeDigestCount(),
        preferenceRepository.preferences,
        notificationRepository.observeManagedNotifications()
    ) { quietMinutes, interruptions, digestCount, preferences, notifications ->
        val today = notifications.filter { it.timestamp >= dayStart }
        val latest = notifications.firstOrNull()
        HomeUiState(
            quietMinutesToday = quietMinutes,
            interruptionsSaved = interruptions,
            digestCount = digestCount,
            filterEnabled = preferences.aiFilterEnabled,
            digestIntervalHours = preferences.digestIntervalHours,
            totalManagedToday = today.size,
            urgentToday = today.count { it.priorityTier == PriorityTier.URGENT },
            importantToday = today.count { it.priorityTier == PriorityTier.IMPORTANT },
            lowToday = today.count { it.priorityTier == PriorityTier.LOW },
            bufferedForDigest = notifications.count {
                !it.isDigested && it.priorityTier in digestTiers
            },
            latestNotification = latest?.let {
                HomeNotificationPreview(
                    appName = it.packageName.substringAfterLast('.').replaceFirstChar(Char::titlecase),
                    sender = it.sender,
                    title = it.title.ifBlank { it.body },
                    tier = it.priorityTier,
                    timestamp = it.timestamp
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

data class HomeUiState(
    val quietMinutesToday: Int = 0,
    val interruptionsSaved: Int = 0,
    val digestCount: Int = 0,
    val filterEnabled: Boolean = true,
    val digestIntervalHours: Int = 1,
    val totalManagedToday: Int = 0,
    val urgentToday: Int = 0,
    val importantToday: Int = 0,
    val lowToday: Int = 0,
    val bufferedForDigest: Int = 0,
    val latestNotification: HomeNotificationPreview? = null
)

data class HomeNotificationPreview(
    val appName: String,
    val sender: String?,
    val title: String,
    val tier: PriorityTier,
    val timestamp: Long
)

private val digestTiers = setOf(PriorityTier.IMPORTANT, PriorityTier.LOW)
