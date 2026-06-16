package com.attentionmanager.domain.usecase

import com.attentionmanager.domain.model.DigestItem
import com.attentionmanager.domain.model.PriorityTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DigestSummaryFormatterTest {
    @Test
    fun `groups buffered notifications by category`() {
        val items = listOf(
            digestItem(1, "news updates"),
            digestItem(2, "news updates"),
            digestItem(3, "news updates"),
            digestItem(4, "LinkedIn reactions"),
            digestItem(5, "LinkedIn reactions"),
            digestItem(6, "promotional emails")
        )

        val summary = DigestSummaryFormatter().format(items)

        assertEquals("3 news updates, 2 LinkedIn reactions, 1 promotional emails", summary.text)
        assertEquals(
            listOf(
                "App 4: Title 4 - Body 4",
                "App 5: Title 5 - Body 5",
                "App 1: Title 1 - Body 1",
                "App 2: Title 2 - Body 2",
                "App 3: Title 3 - Body 3",
                "App 6: Title 6 - Body 6"
            ),
            summary.detailLines
        )
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), summary.notificationIds)
    }

    private fun digestItem(id: Long, category: String) = DigestItem(
        notificationId = id,
        packageName = "pkg.$id",
        appLabel = "App $id",
        category = category,
        title = "Title $id",
        body = "Body $id",
        priorityTier = PriorityTier.LOW
    )
}
