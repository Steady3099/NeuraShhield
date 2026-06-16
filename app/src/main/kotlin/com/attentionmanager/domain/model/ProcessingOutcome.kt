package com.attentionmanager.domain.model

data class ProcessingOutcome(
    val notificationId: Long?,
    val decision: ClassificationDecision,
    val otpCode: String? = null,
    val shouldAutoHide: Boolean = false,
    val spamScore: Float = 0f
) {
    val shouldSilenceOriginal: Boolean =
        decision.tier != PriorityTier.URGENT
}
