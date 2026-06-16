package com.attentionmanager.domain.repository

import androidx.paging.PagingData
import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.domain.model.PriorityTier
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeManagedNotifications(): Flow<List<NotificationEntity>>
    fun observeManagedNotificationsPaged(): Flow<PagingData<NotificationEntity>>
    fun observeQuietMinutesToday(dayStartMillis: Long): Flow<Int>
    fun observeInterruptionCountSince(dayStartMillis: Long): Flow<Int>
    suspend fun insert(notification: NotificationEntity): Long
    suspend fun markRemoved(packageName: String, timestamp: Long)
    suspend fun markRead(id: Long, isRead: Boolean)
    suspend fun markDigested(ids: List<Long>)
    suspend fun setTier(id: Long, tier: PriorityTier, wasPromoted: Boolean, wasDemoted: Boolean)
    suspend fun clearUserFeedbackFlags()
    suspend fun digestCandidates(limit: Int = 200): List<NotificationEntity>
    suspend fun countFromSenderSince(sender: String, sinceMillis: Long): Int
}
