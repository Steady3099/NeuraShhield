package com.attentionmanager.data.repository

import com.attentionmanager.data.database.NotificationDao
import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.domain.model.PriorityTier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotificationRepositoryImplTest {
    @Test
    fun `updates existing notification row when same message is reposted`() = runTest {
        val dao = mockk<NotificationDao>(relaxed = true)
        coEvery {
            dao.findDuplicatePostedNotificationId(
                notificationKey = "posted-key",
                packageName = "com.whatsapp",
                title = "Alex",
                body = "hello",
                sender = "Alex"
            )
        } returns 7L
        val repository = NotificationRepositoryImpl(dao)

        val id = repository.insert(notification(notificationKey = "posted-key"))

        assertEquals(7L, id)
        coVerify(exactly = 1) {
            dao.updatePostedNotification(
                id = 7L,
                packageName = "com.whatsapp",
                title = "Alex",
                body = "hello",
                sender = "Alex",
                timestamp = 1234L,
                priorityTier = PriorityTier.LOW
            )
        }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `inserts notification when key has not been seen`() = runTest {
        val dao = mockk<NotificationDao>(relaxed = true)
        coEvery {
            dao.findDuplicatePostedNotificationId(any(), any(), any(), any(), any())
        } returns null
        coEvery { dao.insert(any()) } returns 11L
        val repository = NotificationRepositoryImpl(dao)

        val id = repository.insert(notification(notificationKey = "new-key"))

        assertEquals(11L, id)
        coVerify(exactly = 1) { dao.insert(any()) }
        coVerify(exactly = 0) {
            dao.updatePostedNotification(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `inserts new row when same key carries different message body`() = runTest {
        val dao = mockk<NotificationDao>(relaxed = true)
        coEvery {
            dao.findDuplicatePostedNotificationId(
                notificationKey = "conversation-key",
                packageName = "com.whatsapp",
                title = "Alex",
                body = "urgent please call now",
                sender = "Alex"
            )
        } returns null
        coEvery { dao.insert(any()) } returns 12L
        val repository = NotificationRepositoryImpl(dao)

        val id = repository.insert(
            notification(
                notificationKey = "conversation-key",
                body = "urgent please call now",
                priorityTier = PriorityTier.URGENT
            )
        )

        assertEquals(12L, id)
        coVerify(exactly = 1) { dao.insert(any()) }
        coVerify(exactly = 0) {
            dao.updatePostedNotification(any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun notification(
        notificationKey: String,
        body: String = "hello",
        priorityTier: PriorityTier = PriorityTier.LOW
    ): NotificationEntity =
        NotificationEntity(
            packageName = "com.whatsapp",
            title = "Alex",
            body = body,
            sender = "Alex",
            timestamp = 1234L,
            notificationKey = notificationKey,
            priorityTier = priorityTier
        )
}
