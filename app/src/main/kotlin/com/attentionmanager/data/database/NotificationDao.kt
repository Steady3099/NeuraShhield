package com.attentionmanager.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attentionmanager.domain.model.PriorityTier
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationEntity): Long

    @Query(
        """
        SELECT id FROM notifications
        WHERE notificationKey = :notificationKey
            AND packageName = :packageName
            AND title = :title
            AND body = :body
            AND IFNULL(sender, '') = IFNULL(:sender, '')
        LIMIT 1
        """
    )
    suspend fun findDuplicatePostedNotificationId(
        notificationKey: String,
        packageName: String,
        title: String,
        body: String,
        sender: String?
    ): Long?

    @Query(
        """
        UPDATE notifications
        SET packageName = :packageName,
            title = :title,
            body = :body,
            sender = :sender,
            timestamp = :timestamp,
            priorityTier = :priorityTier,
            removedAt = NULL
        WHERE id = :id
        """
    )
    suspend fun updatePostedNotification(
        id: Long,
        packageName: String,
        title: String,
        body: String,
        sender: String?,
        timestamp: Long,
        priorityTier: PriorityTier
    )

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeManagedNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun pagingSource(): PagingSource<Int, NotificationEntity>

    @Query(
        """
        SELECT * FROM notifications
        WHERE isDigested = 0 AND priorityTier IN ('LOW', 'IMPORTANT')
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun digestCandidates(limit: Int): List<NotificationEntity>

    @Query("UPDATE notifications SET removedAt = :removedAt WHERE packageName = :packageName AND timestamp = :timestamp")
    suspend fun markRemoved(packageName: String, timestamp: Long, removedAt: Long)

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun markRead(id: Long, isRead: Boolean)

    @Query("UPDATE notifications SET isDigested = 1 WHERE id IN (:ids)")
    suspend fun markDigested(ids: List<Long>)

    @Query(
        """
        UPDATE notifications
        SET priorityTier = :tier,
            wasUserPromoted = :wasPromoted,
            wasUserDemoted = :wasDemoted
        WHERE id = :id
        """
    )
    suspend fun setTier(id: Long, tier: PriorityTier, wasPromoted: Boolean, wasDemoted: Boolean)

    @Query(
        """
        UPDATE notifications
        SET wasUserPromoted = 0,
            wasUserDemoted = 0
        WHERE wasUserPromoted = 1 OR wasUserDemoted = 1
        """
    )
    suspend fun clearUserFeedbackFlags()

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE timestamp >= :dayStartMillis AND priorityTier != 'URGENT'
        """
    )
    fun observeInterruptionCountSince(dayStartMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) * 3 FROM notifications
        WHERE timestamp >= :dayStartMillis AND priorityTier != 'URGENT'
        """
    )
    fun observeQuietMinutesToday(dayStartMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE sender = :sender AND timestamp >= :sinceMillis
        """
    )
    suspend fun countFromSenderSince(sender: String, sinceMillis: Long): Int
}
