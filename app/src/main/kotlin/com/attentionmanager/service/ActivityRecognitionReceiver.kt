package com.attentionmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.attentionmanager.domain.model.ActivityType
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || !ActivityRecognitionResult.hasResult(intent)) return
        val activity = ActivityRecognitionResult.extractResult(intent)?.mostProbableActivity ?: return
        ContextSignalStore.activityType.value = when (activity.type) {
            DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING -> ActivityType.ON_FOOT
            DetectedActivity.STILL -> ActivityType.STILL
            else -> ActivityType.UNKNOWN
        }
    }
}
