package com.attentionmanager.domain.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProcessingOutcomeTest {
    @Test
    fun `urgent notifications are not silenced by default`() {
        val outcome = outcomeFor(PriorityTier.URGENT)

        assertFalse(outcome.shouldSilenceOriginal)
    }

    @Test
    fun `important notifications are silenced for managed digest`() {
        val outcome = outcomeFor(PriorityTier.IMPORTANT)

        assertTrue(outcome.shouldSilenceOriginal)
    }

    @Test
    fun `low notifications are silenced for managed digest`() {
        val outcome = outcomeFor(PriorityTier.LOW)

        assertTrue(outcome.shouldSilenceOriginal)
    }

    @Test
    fun `urgent notifications remain visible even when spam detector flags them`() {
        val outcome = outcomeFor(PriorityTier.URGENT, shouldAutoHide = true)

        assertFalse(outcome.shouldSilenceOriginal)
    }

    @Test
    fun `spam is silenced when it is not urgent`() {
        val outcome = outcomeFor(PriorityTier.LOW, shouldAutoHide = true)

        assertTrue(outcome.shouldSilenceOriginal)
    }

    private fun outcomeFor(
        tier: PriorityTier,
        shouldAutoHide: Boolean = false
    ): ProcessingOutcome =
        ProcessingOutcome(
            notificationId = 1L,
            decision = ClassificationDecision(
                tier = tier,
                confidence = 0.9f,
                source = DecisionSource.TFLITE
            ),
            shouldAutoHide = shouldAutoHide
        )
}
