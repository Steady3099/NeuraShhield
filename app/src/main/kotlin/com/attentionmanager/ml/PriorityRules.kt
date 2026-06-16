package com.attentionmanager.ml

import com.attentionmanager.domain.model.PriorityTier

object PriorityRules {
    data class Match(
        val tier: PriorityTier,
        val patternName: String,
        val confidence: Float
    )

    val urgentPatterns: List<NamedRegex> = listOf(
        NamedRegex("otp_2fa_code", Regex("""(?i)\b(otp|2fa|verification|security|login)\b.{0,32}\b\d{4,8}\b""")),
        NamedRegex("expires", Regex("""(?i)\b(expires?|expiring|valid for|deadline)\b.{0,36}\b(today|tomorrow|\d{1,3}\s*(min|mins|minutes|hr|hour|hours|day|days))\b""")),
        NamedRegex("emergency", Regex("""(?i)\b(emergency|urgent|critical|immediately|asap)\b""")),
        NamedRegex("meeting_in", Regex("""(?i)\b(meeting|call|standup|interview)\s+(starts\s+)?in\s+\d{1,3}\s*(min|mins|minutes|hr|hour)s?\b""")),
        NamedRegex("bank_transaction", Regex("""(?i)\b(debited|credited|transaction|spent|withdrawn|charged|payment)\b.{0,40}\b(bank|card|account|upi|visa|mastercard|amex)\b"""))
    )

    val lowPatterns: List<NamedRegex> = listOf(
        NamedRegex("unsubscribe", Regex("""(?i)\bunsubscribe\b""")),
        NamedRegex("offer_ends", Regex("""(?i)\boffer\s+ends\b""")),
        NamedRegex("promo_expires", Regex("""(?i)\b(offer|sale|deal|coupon|promo|discount)\b.{0,36}\b(expires?|expiring|valid for|ends)\b""")),
        NamedRegex("percent_off", Regex("""(?i)\b\d{1,2}\s*%\s*off\b""")),
        NamedRegex("social_reaction", Regex("""(?i)\b(liked|reacted|commented|shared|followed|mentioned you|new follower)\b""")),
        NamedRegex("promo_language", Regex("""(?i)\b(sale|coupon|deal|limited time|clearance)\b"""))
    )

    fun preClassify(title: String, body: String, sender: String? = null): Match? {
        val text = listOfNotNull(title, sender, body).joinToString(" ")
        lowPatterns.firstOrNull { it.regex.containsMatchIn(text) }?.let {
            if (it.name == "promo_expires") return Match(PriorityTier.LOW, it.name, confidence = 0.94f)
        }
        urgentPatterns.firstOrNull { it.regex.containsMatchIn(text) }?.let {
            return Match(PriorityTier.URGENT, it.name, confidence = 0.98f)
        }
        lowPatterns.firstOrNull { it.regex.containsMatchIn(text) }?.let {
            return Match(PriorityTier.LOW, it.name, confidence = 0.92f)
        }
        return null
    }

    data class NamedRegex(val name: String, val regex: Regex)
}
