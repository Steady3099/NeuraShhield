package com.attentionmanager.domain.usecase

import com.attentionmanager.domain.model.DigestItem
import com.attentionmanager.domain.model.DigestSummary

class DigestSummaryFormatter {
    fun format(items: List<DigestItem>): DigestSummary {
        if (items.isEmpty()) {
            return DigestSummary("No buffered notifications.", emptyList(), emptyList())
        }
        val phrases = items
            .groupBy { it.category }
            .entries
            .sortedByDescending { it.value.size }
            .map { (category, categoryItems) -> phrase(categoryItems.size, category) }

        return DigestSummary(
            text = phrases.joinToString(separator = ", "),
            detailLines = items
                .sortedWith(compareBy<DigestItem> { it.category }.thenBy { it.appLabel })
                .take(MAX_DETAIL_LINES)
                .map { it.toDetailLine() },
            notificationIds = items.map { it.notificationId }
        )
    }

    private fun phrase(count: Int, category: String): String {
        val normalized = category.ifBlank { "updates" }
        val suffix = if (count == 1 || normalized.endsWith("s")) normalized else "${normalized}s"
        return "$count $suffix"
    }

    private fun DigestItem.toDetailLine(): String {
        val headline = title.ifBlank { body }.cleanForNotification()
        val preview = body
            .takeUnless { it.isBlank() || it.equals(title, ignoreCase = true) }
            ?.cleanForNotification()
            ?.let { " - $it" }
            .orEmpty()
        return "${appLabel.cleanForNotification()}: ${headline.take(64)}${preview.take(64)}"
            .take(140)
    }

    private fun String.cleanForNotification(): String =
        replace(Regex("\\s+"), " ").trim()

    companion object {
        private const val MAX_DETAIL_LINES = 7
    }
}
