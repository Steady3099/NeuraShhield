package com.attentionmanager.ml

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpamDetectorTest {
    @Test
    fun `auto hides lottery link scam`() {
        val result = SpamDetector.score(
            title = "You won",
            body = "Click here to claim your exclusive lottery prize!!! Limited time offer, buy now and claim now!!!",
            repeatedSenderCountLastHour = 0
        )

        assertTrue(result.shouldAutoHide)
        assertTrue(result.reasons.contains("scam prize language"))
        assertTrue(result.reasons.contains("link bait"))
    }

    @Test
    fun `does not auto hide normal package update with reference number`() {
        val result = SpamDetector.score(
            title = "Package update",
            body = "Tracking FD-38568 now shows delivery tomorrow.",
            repeatedSenderCountLastHour = 0
        )

        assertFalse(result.shouldAutoHide)
    }
}
