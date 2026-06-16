package com.attentionmanager.ml

import com.attentionmanager.domain.model.ActivityType
import com.attentionmanager.domain.model.AppContext
import com.attentionmanager.domain.model.DecisionSource
import com.attentionmanager.domain.model.PriorityTier
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationClassifierTest {
    @Test
    fun `uses model probabilities for ambiguous notification`() {
        val runner = mockk<NotificationModelRunner>(relaxed = true)
        every { runner.classify(any()) } returns floatArrayOf(0.82f, 0.12f, 0.06f)

        val classifier = NotificationClassifier { runner }
        val decision = classifier.classify(
            title = "Heads up",
            body = "Please review this when you can",
            sender = "Alex",
            senderBoost = 0f,
            appContext = AppContext(activityType = ActivityType.STILL, currentHour = 10)
        )

        assertEquals(PriorityTier.URGENT, decision.tier)
        assertEquals(DecisionSource.TFLITE, decision.source)
    }

    @Test
    fun `does not promote flat model output to urgent`() {
        val runner = mockk<NotificationModelRunner>(relaxed = true)
        every { runner.classify(any()) } returns floatArrayOf(0.39f, 0.32f, 0.29f)

        val classifier = NotificationClassifier { runner }
        val decision = classifier.classify(
            title = "Heads up",
            body = "Package update available for tomorrow",
            sender = "Store",
            senderBoost = 0f,
            appContext = AppContext(activityType = ActivityType.STILL, currentHour = 10)
        )

        assertEquals(PriorityTier.LOW, decision.tier)
        assertEquals(DecisionSource.TFLITE, decision.source)
    }

    @Test
    fun `requires high confidence before model marks urgent`() {
        val runner = mockk<NotificationModelRunner>(relaxed = true)
        every { runner.classify(any()) } returns floatArrayOf(0.58f, 0.21f, 0.21f)

        val classifier = NotificationClassifier { runner }
        val decision = classifier.classify(
            title = "Heads up",
            body = "Please review this when you can",
            sender = "Alex",
            senderBoost = 0f,
            appContext = AppContext(activityType = ActivityType.STILL, currentHour = 10)
        )

        assertEquals(PriorityTier.LOW, decision.tier)
    }

    @Test
    fun `vehicle context blocks non urgent model result`() {
        val runner = mockk<NotificationModelRunner>(relaxed = true)
        every { runner.classify(any()) } returns floatArrayOf(0.2f, 0.75f, 0.05f)

        val classifier = NotificationClassifier { runner }
        val decision = classifier.classify(
            title = "Project update",
            body = "Build finished successfully",
            sender = "CI",
            senderBoost = 0f,
            appContext = AppContext(activityType = ActivityType.IN_VEHICLE, currentHour = 16)
        )

        assertEquals(PriorityTier.LOW, decision.tier)
    }
}
