package com.attentionmanager.ml

import com.attentionmanager.domain.model.PriorityTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PriorityRulesTest {
    @ParameterizedTest
    @CsvSource(
        "Security,Your OTP is 123456,URGENT",
        "Calendar,Meeting in 10 minutes,URGENT",
        "Bank,Your card was charged by bank,URGENT",
        "Ops,Emergency incident declared,URGENT",
        "Trial,Your invite expires today,URGENT",
        "Shop,Offer expires today,LOW",
        "Shop,Offer ends tonight,LOW",
        "Mail,Unsubscribe from these alerts,LOW",
        "Store,40% off shoes today,LOW",
        "Social,Alex liked your post,LOW"
    )
    fun `classifies known priority patterns`(title: String, body: String, tierName: String) {
        assertEquals(
            PriorityTier.fromStorage(tierName),
            PriorityRules.preClassify(title, body)?.tier
        )
    }

    @ParameterizedTest
    @CsvSource(
        "Build,Finished successfully",
        "Chat,Can you review the doc later",
        "Weather,Rain likely after 6 PM"
    )
    fun `leaves ambiguous messages for ml`(title: String, body: String) {
        assertNull(PriorityRules.preClassify(title, body))
    }
}
