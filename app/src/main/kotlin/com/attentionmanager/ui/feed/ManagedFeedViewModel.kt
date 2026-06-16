package com.attentionmanager.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.usecase.UserFeedbackUseCase
import kotlinx.coroutines.launch

class ManagedFeedViewModel(
    notificationRepository: NotificationRepository,
    private val userFeedbackUseCase: UserFeedbackUseCase
) : ViewModel() {
    val notifications = notificationRepository.observeManagedNotificationsPaged()
        .cachedIn(viewModelScope)

    fun promote(notification: NotificationEntity) {
        val sender = notification.sender ?: notification.title
        viewModelScope.launch {
            userFeedbackUseCase.alwaysUrgentFromSender(notification.id, sender, notification.packageName)
        }
    }

    fun demote(notification: NotificationEntity) {
        viewModelScope.launch {
            userFeedbackUseCase.alwaysLowFromApp(notification.id, notification.packageName)
        }
    }

    fun dismissDontLearn(notification: NotificationEntity) {
        viewModelScope.launch { userFeedbackUseCase.dismissDontLearn(notification.id) }
    }

    fun reportSpam(notification: NotificationEntity) {
        viewModelScope.launch {
            userFeedbackUseCase.reportAsSpam(
                notificationId = notification.id,
                packageName = notification.packageName,
                sender = notification.sender,
                title = notification.title
            )
        }
    }
}
