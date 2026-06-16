package com.attentionmanager.domain.usecase

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.attentionmanager.service.DigestWorker
import java.util.concurrent.TimeUnit

class DigestScheduler(private val context: Context) {
    fun syncDigestSchedule(intervalHours: Int) {
        val workManager = WorkManager.getInstance(context)
        val clampedInterval = intervalHours.coerceIn(1, 24)
        workManager.cancelAllWorkByTag(TAG)
        val request = PeriodicWorkRequestBuilder<DigestWorker>(clampedInterval.toLong(), TimeUnit.HOURS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val TAG = "attention-digest"
        private const val UNIQUE_WORK_NAME = "digest-interval"
    }
}
