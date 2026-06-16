package com.attentionmanager.domain.model

data class NotificationPayload(
    val packageName: String,
    val title: String,
    val body: String,
    val sender: String?,
    val timestamp: Long,
    val notificationKey: String,
    val isGroupSummary: Boolean = false
) {
    val combinedText: String = listOfNotNull(title, sender, body)
        .filter { it.isNotBlank() }
        .joinToString(separator = " ")
}
