package com.attentionmanager.ml

data class SpamResult(
    val score: Float,
    val reasons: List<String>
) {
    val shouldAutoHide: Boolean = score > 0.95f
}

object SpamDetector {
    private val promotionalKeywords = listOf(
        "buy now",
        "limited time",
        "exclusive offer",
        "coupon",
        "promo",
        "sale",
        "clearance",
        "free shipping",
        "claim now"
    )
    private val scamKeywords = listOf(
        "lottery",
        "jackpot",
        "prize",
        "winner",
        "you won",
        "win cash",
        "free money",
        "claim reward",
        "claim your reward",
        "claim your prize"
    )
    private val linkBait = Regex("""(?i)\b(click\s+(here|this|the)?\s*(link)?|tap\s+(here|this|the)?\s*(link)?|open\s+link|claim\s+(now|here))\b""")
    private val suspiciousUrl = Regex("""(?i)\b(https?://|bit\.ly|tinyurl|t\.co|wa\.me|shorturl|linktr\.ee)\b""")
    private val excessivePunctuation = Regex("""[!?]{3,}""")

    fun score(
        title: String,
        body: String,
        repeatedSenderCountLastHour: Int
    ): SpamResult {
        val text = "$title $body"
        val reasons = mutableListOf<String>()
        var score = 0f

        val keywordHits = promotionalKeywords.count { text.contains(it, ignoreCase = true) }
        if (keywordHits > 0) {
            score += (keywordHits * 0.22f).coerceAtMost(0.58f)
            reasons += "promotional keywords"
        }
        val scamHits = scamKeywords.count { text.contains(it, ignoreCase = true) }
        if (scamHits > 0) {
            score += (scamHits * 0.24f).coerceAtMost(0.48f)
            reasons += "scam prize language"
        }
        if (linkBait.containsMatchIn(text)) {
            score += 0.28f
            reasons += "link bait"
        }
        if (suspiciousUrl.containsMatchIn(text)) {
            score += 0.18f
            reasons += "suspicious link"
        }
        if (excessivePunctuation.containsMatchIn(text)) {
            score += 0.18f
            reasons += "excessive punctuation"
        }
        if (scamHits > 0 && linkBait.containsMatchIn(text)) {
            score += 0.22f
            reasons += "scam call to action"
        }
        if (repeatedSenderCountLastHour >= 3) {
            score += 0.32f
            reasons += "repeated sender in 1 hour"
        }

        return SpamResult(score.coerceIn(0f, 1f), reasons.distinct())
    }
}
