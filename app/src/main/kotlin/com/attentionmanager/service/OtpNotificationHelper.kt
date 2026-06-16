package com.attentionmanager.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.attentionmanager.R

class OtpNotificationHelper(private val context: Context) {
    fun copyAndNotify(code: String) {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("OTP", code))
        Toast.makeText(context, "OTP copied", Toast.LENGTH_SHORT).show()
        postDismissNotification()
    }

    @SuppressLint("MissingPermission")
    private fun postDismissNotification() {
        if (!canPostNotifications()) return
        ensureChannel()
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            OTP_NOTIFICATION_ID,
            Intent(context, DismissOtpReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("OTP copied")
            .setContentText("The code was copied to your clipboard.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .addAction(0, "Dismiss", dismissIntent)
            .build()
        NotificationManagerCompat.from(context).notify(OTP_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.otp_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val CHANNEL_ID = "otp_helper"
        const val OTP_NOTIFICATION_ID = 8801
    }
}
