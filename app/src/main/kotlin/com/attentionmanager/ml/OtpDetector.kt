package com.attentionmanager.ml

object OtpDetector {
    private val anchoredCode = Regex(
        pattern = """(?i)\b(?:otp|2fa|verification|security|login|code|pin)\b[^\d]{0,24}(\d{4,8})\b"""
    )
    private val trailingContext = Regex(
        pattern = """(?i)\b(\d{4,8})\b[^\n]{0,24}\b(?:otp|2fa|verification|security|login|code|pin)\b"""
    )

    fun detect(body: String): OtpDetection? {
        val direct = anchoredCode.find(body)?.groups?.get(1)?.value
        val contextual = trailingContext.find(body)?.groups?.get(1)?.value
        val code = direct ?: contextual ?: return null
        return OtpDetection(code = code, sourceText = body)
    }
}

data class OtpDetection(
    val code: String,
    val sourceText: String
)
