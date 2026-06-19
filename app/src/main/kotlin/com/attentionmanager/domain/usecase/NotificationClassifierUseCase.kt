package com.attentionmanager.domain.usecase

import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.data.database.SpamLogEntity
import com.attentionmanager.domain.model.AttentionError
import com.attentionmanager.domain.model.AttentionResult
import com.attentionmanager.domain.model.ActivityType
import com.attentionmanager.domain.model.AppContext
import com.attentionmanager.domain.model.ClassificationDecision
import com.attentionmanager.domain.model.DecisionSource
import com.attentionmanager.domain.model.NotificationPayload
import com.attentionmanager.domain.model.PriorityTier
import com.attentionmanager.domain.model.ProcessingOutcome
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import com.attentionmanager.ml.NotificationClassifier
import com.attentionmanager.ml.OtpDetector
import com.attentionmanager.ml.PriorityRules
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
                val appContext = runCatching {
                    contextSignalProvider.currentContext.first()
                }.getOrDefault(AppContext())
                val boost = runCatching {
                    contactPriorityRepository.boostFor(payload.sender, payload.packageName)
                }.getOrDefault(0f)
                val decision = runCatching {
                    classifier.classify(
                        title = payload.title,
                        body = payload.body,
                        sender = payload.sender,
                        senderBoost = boost,
                        appContext = appContext
                    )
                }.getOrElse {
                    fallbackDecision(payload, appContext)
                }
                val entityId = if (payload.isGroupSummary) {
                    null
                } else {
                    runCatching {
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
                    }.getOrNull()
                }

                val repeatedSenderCount = if (payload.isGroupSummary) {
                    0
                } else {
                    runCatching {
                        payload.sender
                            ?.let { notificationRepository.countFromSenderSince(it, payload.timestamp - ONE_HOUR_MILLIS) }
                            ?: 0
                    }.getOrDefault(0)
                }
                val spam = SpamDetector.score(payload.title, payload.body, repeatedSenderCount)
                if (spam.shouldAutoHide && entityId != null) {
                    runCatching {
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

    private fun fallbackDecision(payload: NotificationPayload, appContext: AppContext): ClassificationDecision =
        PriorityRules.preClassify(payload.title, payload.body, payload.sender)
            ?.let { ClassificationDecision(it.tier, it.confidence, DecisionSource.REGEX) }
            ?: ClassificationDecision(
                tier = if (appContext.activityType == ActivityType.IN_VEHICLE) {
                    PriorityTier.LOW
                } else {
                    PriorityTier.IMPORTANT
                },
                confidence = 0.5f,
                source = DecisionSource.FALLBACK
            )

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
