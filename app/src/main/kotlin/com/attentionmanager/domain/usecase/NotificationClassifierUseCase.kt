package com.attentionmanager.domain.usecase

import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.data.database.SpamLogEntity
import com.attentionmanager.domain.model.AttentionError
import com.attentionmanager.domain.model.AttentionResult
import com.attentionmanager.domain.model.NotificationPayload
import com.attentionmanager.domain.model.ProcessingOutcome
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import com.attentionmanager.ml.NotificationClassifier
import com.attentionmanager.ml.OtpDetector
import com.attentionmanager.ml.SpamDetector
import com.attentionmanager.service.ContextSignalProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class NotificationClassifierUseCase(
    private val notificationRepository: NotificationRepository,
    private val contactPriorityRepository: ContactPriorityRepository,
    private val spamLogRepository: SpamLogRepository,
    private val classifier: NotificationClassifier,
    private val contextSignalProvider: ContextSignalProvider
) {
    suspend fun process(payload: NotificationPayload): AttentionResult<ProcessingOutcome> =
        withContext(Dispatchers.Default) {
            try {
                val appContext = contextSignalProvider.currentContext.first()
                val boost = contactPriorityRepository.boostFor(payload.sender, payload.packageName)
                val decision = classifier.classify(
                    title = payload.title,
                    body = payload.body,
                    sender = payload.sender,
                    senderBoost = boost,
                    appContext = appContext
                )
                val entityId = if (payload.isGroupSummary) {
                    null
                } else {
                    notificationRepository.insert(
                        NotificationEntity(
                            packageName = payload.packageName,
                            title = payload.title,
                            body = payload.body,
                            sender = payload.sender,
                            timestamp = payload.timestamp,
                            notificationKey = payload.notificationKey,
                            priorityTier = decision.tier
                        )
                    )
                }

                val repeatedSenderCount = if (payload.isGroupSummary) {
                    0
                } else {
                    payload.sender
                        ?.let { notificationRepository.countFromSenderSince(it, payload.timestamp - ONE_HOUR_MILLIS) }
                        ?: 0
                }
                val spam = SpamDetector.score(payload.title, payload.body, repeatedSenderCount)
                if (spam.shouldAutoHide && entityId != null) {
                    spamLogRepository.insert(
                        SpamLogEntity(
                            notificationId = entityId,
                            packageName = payload.packageName,
                            sender = payload.sender,
                            title = payload.title,
                            timestamp = payload.timestamp,
                            score = spam.score,
                            reasons = spam.reasons
                        )
                    )
                }

                AttentionResult.Success(
                    ProcessingOutcome(
                        notificationId = entityId,
                        decision = decision,
                        otpCode = OtpDetector.detect(payload.body)?.code,
                        shouldAutoHide = spam.shouldAutoHide,
                        spamScore = spam.score
                    )
                )
            } catch (throwable: Throwable) {
                AttentionResult.Failure(
                    AttentionError.Unknown(
                        message = "Unable to classify notification.",
                        cause = throwable
                    )
                )
            }
        }

    suspend fun onRemoved(packageName: String, timestamp: Long): AttentionResult<Unit> =
        withContext(Dispatchers.Default) {
            try {
                notificationRepository.markRemoved(packageName, timestamp)
                AttentionResult.Success(Unit)
            } catch (throwable: Throwable) {
                AttentionResult.Failure(AttentionError.Database("Unable to mark notification removed.", throwable))
            }
        }

    companion object {
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }
}
