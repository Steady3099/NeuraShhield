package com.attentionmanager.domain.usecase

import com.attentionmanager.data.database.ContactPriorityEntity
import com.attentionmanager.data.database.SpamLogEntity
import com.attentionmanager.domain.model.PriorityTier
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserFeedbackUseCase(
    private val notificationRepository: NotificationRepository,
    private val contactPriorityRepository: ContactPriorityRepository,
    private val spamLogRepository: SpamLogRepository,
    private val modelTrainer: LocalModelTrainer
) {
    suspend fun alwaysUrgentFromSender(notificationId: Long, sender: String, packageName: String) {
        contactPriorityRepository.upsert(
            ContactPriorityEntity(
                contactId = contactId(packageName, sender),
                displayName = sender,
                packageName = packageName,
                priorityBoost = 1f,
                lastUpdated = System.currentTimeMillis()
            )
        )
        notificationRepository.setTier(notificationId, PriorityTier.URGENT, wasPromoted = true, wasDemoted = false)
        modelTrainer.scheduleIncrementalRetraining()
    }

    suspend fun alwaysLowFromApp(notificationId: Long, packageName: String) {
        contactPriorityRepository.upsert(
            ContactPriorityEntity(
                contactId = "app:$packageName",
                displayName = "All from $packageName",
                packageName = packageName,
                priorityBoost = -1f,
                lastUpdated = System.currentTimeMillis()
            )
        )
        notificationRepository.setTier(notificationId, PriorityTier.LOW, wasPromoted = false, wasDemoted = true)
        modelTrainer.scheduleIncrementalRetraining()
    }

    suspend fun dismissDontLearn(notificationId: Long) {
        notificationRepository.markRead(notificationId, true)
    }

    suspend fun reportAsSpam(
        notificationId: Long,
        packageName: String,
        sender: String?,
        title: String
    ) {
        spamLogRepository.insert(
            SpamLogEntity(
                notificationId = notificationId,
                packageName = packageName,
                sender = sender,
                title = title,
                timestamp = System.currentTimeMillis(),
                score = 1f,
                reasons = listOf("user reported")
            )
        )
        notificationRepository.setTier(notificationId, PriorityTier.LOW, wasPromoted = false, wasDemoted = true)
        modelTrainer.scheduleIncrementalRetraining()
    }

    suspend fun resetAiModel() {
        withContext(Dispatchers.Default) {
            contactPriorityRepository.clearAll()
            notificationRepository.clearUserFeedbackFlags()
            spamLogRepository.clearAll()
            modelTrainer.resetPersonalization()
        }
    }

    private fun contactId(packageName: String, sender: String): String =
        "$packageName:${sender.trim().lowercase()}"
}

interface LocalModelTrainer {
    suspend fun scheduleIncrementalRetraining()
    suspend fun resetPersonalization()
}

class NoOpLocalModelTrainer : LocalModelTrainer {
    override suspend fun scheduleIncrementalRetraining() = Unit
    override suspend fun resetPersonalization() = Unit
}
