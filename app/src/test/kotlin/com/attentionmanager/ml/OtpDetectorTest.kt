package com.attentionmanager.ml

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class OtpDetectorTest {
    @ParameterizedTest
    @CsvSource(
        "Your OTP is 1234,1234",
        "Use verification code 567890,567890",
        "2FA code: 246810,246810",
        "Security code 999999 expires in 5 minutes,999999",
        "Login PIN 4321 for NeuraShhield,4321",
        "Your one time code is 112233,112233",
        "Code 12345678 is valid once,12345678",
        "verification: 0000,0000",
        "Use 888888 as your OTP,888888",
        "Enter 135790 for verification,135790",
        "Your login code is 543210. Do not share it,543210",
        "Bank OTP 765432 valid for 3 minutes,765432",
        "PIN: 1212,1212",
        "Security 987654,987654",
        "verification code - 444555,444555",
        "Your code: 7777,7777",
        "2fa 101010,101010",
        "Login code 222333,222333",
        "Use code 555666 to continue,555666",
        "OTP=314159,314159",
        "Your security pin is 808080,808080"
    )
    fun `detects common otp formats`(body: String, expected: String) {
        assertEquals(expected, OtpDetector.detect(body)?.code)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Your order number is 123456 and ships today",
            "Meeting starts at 2026",
            "Invoice total is 1234 INR",
            "Sale ends tonight with 50% off"
        ]
    )
    fun `ignores unrelated numbers`(body: String) {
        assertNull(OtpDetector.detect(body))
    }
}
