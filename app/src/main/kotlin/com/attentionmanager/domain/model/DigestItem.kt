package com.attentionmanager.domain.model

data class DigestItem(
    val notificationId: Long,
    val packageName: String,
    val appLabel: String,
    val category: String,
    val title: String,
    val body: String,
    val priorityTier: PriorityTier
)

data class DigestSummary(
    val text: String,
    val detailLines: List<String>,
    val notificationIds: List<Long>
) {
    val detailText: String = buildString {
        append(text)
        if (detailLines.isNotEmpty()) {
            append("\n\n")
            append(detailLines.joinToString(separator = "\n"))
        }
    }
}
