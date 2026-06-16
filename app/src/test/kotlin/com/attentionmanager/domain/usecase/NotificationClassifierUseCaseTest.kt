package com.attentionmanager.domain.usecase

import com.attentionmanager.domain.model.ActivityType
import com.attentionmanager.domain.model.AppContext
import com.attentionmanager.domain.model.AttentionResult
import com.attentionmanager.domain.model.NotificationPayload
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import com.attentionmanager.ml.NotificationClassifier
import com.attentionmanager.ml.NotificationModelRunner
import com.attentionmanager.service.ContextSignalProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationClassifierUseCaseTest {
    @Test
    fun `group summaries are not persisted to managed feed`() = runTest {
        val notificationRepository = mockk<NotificationRepository>(relaxed = true)
        val contactPriorityRepository = mockk<ContactPriorityRepository>()
        val spamLogRepository = mockk<SpamLogRepository>(relaxed = true)
        val contextSignalProvider = mockk<ContextSignalProvider>()
        every { contextSignalProvider.currentContext } returns flowOf(
            AppContext(activityType = ActivityType.STILL, currentHour = 12)
        )
        coEvery { contactPriorityRepository.boostFor(any(), any()) } returns 0f

        val useCase = NotificationClassifierUseCase(
            notificationRepository = notificationRepository,
            contactPriorityRepository = contactPriorityRepository,
            spamLogRepository = spamLogRepository,
            classifier = NotificationClassifier { FixedRunner(floatArrayOf(0.05f, 0.10f, 0.85f)) },
            contextSignalProvider = contextSignalProvider
        )

        val result = useCase.process(
            NotificationPayload(
                packageName = "com.whatsapp",
                title = "WhatsApp",
                body = "2 messages from 1 chat",
                sender = "WhatsApp",
                timestamp = 1234L,
                notificationKey = "summary-key",
                isGroupSummary = true
            )
        )

        assertTrue(result is AttentionResult.Success)
        val outcome = (result as AttentionResult.Success).value
        assertNull(outcome.notificationId)
        assertTrue(outcome.shouldSilenceOriginal)
        coVerify(exactly = 0) { notificationRepository.insert(any()) }
        coVerify(exactly = 0) { notificationRepository.countFromSenderSince(any(), any()) }
        coVerify(exactly = 0) { spamLogRepository.insert(any()) }
    }

    private class FixedRunner(
        private val probabilities: FloatArray
    ) : NotificationModelRunner {
        override fun classify(tokenIds: IntArray): FloatArray = probabilities
        override fun close() = Unit
    }
}
