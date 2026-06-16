package com.attentionmanager.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attentionmanager.domain.model.PriorityTier

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["packageName"]),
        Index(value = ["sender"]),
        Index(value = ["notificationKey"]),
        Index(value = ["priorityTier"]),
        Index(value = ["isDigested"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val body: String,
    val sender: String?,
    val timestamp: Long,
    val notificationKey: String = "",
    val priorityTier: PriorityTier,
    val isRead: Boolean = false,
    val isDigested: Boolean = false,
    val wasUserPromoted: Boolean = false,
    val wasUserDemoted: Boolean = false,
    val removedAt: Long? = null
)

@Entity(
    tableName = "contact_priorities",
    primaryKeys = ["contactId", "packageName"],
    indices = [Index(value = ["displayName"])]
)
data class ContactPriorityEntity(
    val contactId: String,
    val displayName: String,
    val packageName: String,
    val priorityBoost: Float,
    val lastUpdated: Long
) {
    val clampedPriorityBoost: Float
        get() = priorityBoost.coerceIn(-1f, 1f)
}

@Entity(tableName = "digests")
data class DigestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val generatedAt: Long,
    val summaryText: String,
    val notificationIds: List<Long>
)

@Entity(
    tableName = "spam_log",
    indices = [Index(value = ["timestamp"]), Index(value = ["packageName"])]
)
data class SpamLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationId: Long?,
    val packageName: String,
    val sender: String?,
    val title: String,
    val timestamp: Long,
    val score: Float,
    val reasons: List<String>
)
