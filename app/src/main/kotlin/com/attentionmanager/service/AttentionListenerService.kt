package com.attentionmanager.service

import android.app.Notification
import android.app.Person
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.attentionmanager.AppGraph
import com.attentionmanager.domain.model.AttentionResult
import com.attentionmanager.domain.model.NotificationPayload
import com.attentionmanager.domain.model.ProcessingOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttentionListenerService : NotificationListenerService(), LifecycleOwner {
    private val dispatcher = ServiceLifecycleDispatcher(this)
    private lateinit var graph: AppGraph
    private lateinit var otpNotificationHelper: OtpNotificationHelper

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        graph = AppGraph.from(this)
        otpNotificationHelper = OtpNotificationHelper(this)
        graph.contextSignalProvider.start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        lifecycleScope.launch(Dispatchers.Default) {
            if (!graph.attentionController.isEnabled.first()) return@launch
            if (sbn.packageName == packageName) return@launch
            val payload = sbn.toPayload()
            when (val result = graph.notificationClassifierUseCase.process(payload)) {
                is AttentionResult.Success -> handleOutcome(
                    notificationKey = sbn.key,
                    packageName = sbn.packageName,
                    groupKey = sbn.groupKey,
                    isGroupSummary = sbn.isGroupSummaryNotification(),
                    outcome = result.value
                )
                is AttentionResult.Failure -> Log.w(TAG, result.error.message, result.error.cause)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        lifecycleScope.launch(Dispatchers.Default) {
            graph.notificationClassifierUseCase.onRemoved(sbn.packageName, sbn.postTime)
        }
    }

    private suspend fun handleOutcome(
        notificationKey: String,
        packageName: String,
        groupKey: String,
        isGroupSummary: Boolean,
        outcome: ProcessingOutcome
    ) {
        if (outcome.otpCode != null) {
            withContext(Dispatchers.Main) {
                otpNotificationHelper.copyAndNotify(outcome.otpCode)
            }
        }

        if (outcome.shouldSilenceOriginal) {
            if (isGroupSummary) {
                delay(GROUP_SUMMARY_SETTLE_MS)
                if (!hasActiveChildren(packageName, groupKey)) {
                    silenceNotification(notificationKey)
                }
            } else {
                silenceNotification(notificationKey)
            }
        }
    }

    private fun silenceNotification(notificationKey: String) {
        runCatching { cancelNotification(notificationKey) }
            .onFailure { Log.w(TAG, "Unable to silence non-urgent notification.", it) }
    }

    private fun hasActiveChildren(packageName: String, groupKey: String): Boolean =
        runCatching {
            activeNotifications.orEmpty().any { active ->
                active.packageName == packageName &&
                    active.groupKey == groupKey &&
                    !active.isGroupSummaryNotification()
            }
        }.getOrDefault(false)

    private fun StatusBarNotification.toPayload(): NotificationPayload {
        val extras = notification.extras
        val title = extras.charSequence(Notification.EXTRA_TITLE).orEmpty()
        val body = extras.charSequence(Notification.EXTRA_BIG_TEXT)
            .ifBlank { extras.charSequence(Notification.EXTRA_TEXT) }
            .ifBlank { extras.textLines() }
        return NotificationPayload(
            packageName = packageName,
            title = title,
            body = body,
            sender = extractSender(extras, notification),
            timestamp = postTime,
            notificationKey = key,
            isGroupSummary = isGroupSummaryNotification()
        )
    }

    private fun StatusBarNotification.isGroupSummaryNotification(): Boolean =
        notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

    private fun extractSender(extras: Bundle, notification: Notification): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val messages = BundleCompat.getParcelableArray(
                extras,
                Notification.EXTRA_MESSAGES,
                Bundle::class.java
            )
                ?.mapNotNull { it as? Bundle }
                ?.toList()
                .orEmpty()
            val message = messages.lastOrNull()
            val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                message?.let { BundleCompat.getParcelable(it, "sender_person", Person::class.java) }
                    ?.name
                    ?.toString()
            } else {
                message?.getCharSequence("sender")?.toString()
            }
            if (!sender.isNullOrBlank()) return sender
        }
        return extras.charSequence(Notification.EXTRA_SUB_TEXT)
            .ifBlank { notification.extras.charSequence(Notification.EXTRA_TITLE) }
            .takeUnless { it.isBlank() }
    }

    private fun Bundle.charSequence(key: String): String =
        getCharSequence(key)?.toString().orEmpty()

    private fun String.ifBlank(fallback: () -> String): String =
        if (isBlank()) fallback() else this

    private fun Bundle.textLines(): String =
        getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(separator = "\n") { it.toString() }
            .orEmpty()

    companion object {
        private const val TAG = "AttentionListener"
        private const val GROUP_SUMMARY_SETTLE_MS = 700L
    }
}
