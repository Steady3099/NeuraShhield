package com.attentionmanager.domain.model

data class ClassificationDecision(
    val tier: PriorityTier,
    val confidence: Float,
    val source: DecisionSource,
    val probabilities: FloatArray = FloatArray(3)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassificationDecision) return false
        return tier == other.tier &&
            confidence == other.confidence &&
            source == other.source &&
            probabilities.contentEquals(other.probabilities)
    }

    override fun hashCode(): Int {
        var result = tier.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + probabilities.contentHashCode()
        return result
    }
}

enum class DecisionSource {
    REGEX,
    HEURISTIC,
    TFLITE,
    FALLBACK
}
