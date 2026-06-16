package com.attentionmanager.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.attentionmanager.data.database.NotificationDao
import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.domain.model.PriorityTier
import com.attentionmanager.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {
    override fun observeManagedNotifications(): Flow<List<NotificationEntity>> =
        dao.observeManagedNotifications()

    override fun observeManagedNotificationsPaged(): Flow<PagingData<NotificationEntity>> =
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            dao.pagingSource()
        }.flow

    override fun observeQuietMinutesToday(dayStartMillis: Long): Flow<Int> =
        dao.observeQuietMinutesToday(dayStartMillis)

    override fun observeInterruptionCountSince(dayStartMillis: Long): Flow<Int> =
        dao.observeInterruptionCountSince(dayStartMillis)

    override suspend fun insert(notification: NotificationEntity): Long {
        val existingId = notification.notificationKey
            .takeIf { it.isNotBlank() }
            ?.let {
                dao.findDuplicatePostedNotificationId(
                    notificationKey = it,
                    packageName = notification.packageName,
                    title = notification.title,
                    body = notification.body,
                    sender = notification.sender
                )
            }

        if (existingId != null) {
            dao.updatePostedNotification(
                id = existingId,
                packageName = notification.packageName,
                title = notification.title,
                body = notification.body,
                sender = notification.sender,
                timestamp = notification.timestamp,
                priorityTier = notification.priorityTier
            )
            return existingId
        }

        return dao.insert(notification)
    }

    override suspend fun markRemoved(packageName: String, timestamp: Long) =
        dao.markRemoved(packageName, timestamp, System.currentTimeMillis())

    override suspend fun markRead(id: Long, isRead: Boolean) = dao.markRead(id, isRead)

    override suspend fun markDigested(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.markDigested(ids)
    }

    override suspend fun setTier(
        id: Long,
        tier: PriorityTier,
        wasPromoted: Boolean,
        wasDemoted: Boolean
    ) = dao.setTier(id, tier, wasPromoted, wasDemoted)

    override suspend fun clearUserFeedbackFlags() = dao.clearUserFeedbackFlags()

    override suspend fun digestCandidates(limit: Int): List<NotificationEntity> =
        dao.digestCandidates(limit)

    override suspend fun countFromSenderSince(sender: String, sinceMillis: Long): Int =
        dao.countFromSenderSince(sender, sinceMillis)
}
