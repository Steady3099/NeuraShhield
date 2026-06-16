package com.attentionmanager.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.attentionmanager.R
import com.attentionmanager.domain.model.DigestSummary
import com.attentionmanager.domain.usecase.DigestPublisher

class DigestNotificationPublisher(private val context: Context) : DigestPublisher {
    @SuppressLint("MissingPermission")
    override fun postDigest(summary: DigestSummary) {
        if (!canPostNotifications()) return
        ensureChannel()
        val count = summary.notificationIds.size
        val title = "NeuraShhield digest"
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("$count managed ${if (count == 1) "notification" else "notifications"}")
            .setSummaryText(summary.text)
        summary.detailLines.forEach { inboxStyle.addLine(it) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(summary.text)
            .setSubText("$count items")
            .setNumber(count)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText("$count managed ${if (count == 1) "notification" else "notifications"}")
                    .build()
            )
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DIGEST_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.digest_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val CHANNEL_ID = "digest_notifications"
        private const val DIGEST_NOTIFICATION_ID = 9001
    }
}
