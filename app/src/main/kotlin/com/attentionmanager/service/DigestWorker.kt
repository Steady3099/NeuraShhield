package com.attentionmanager.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attentionmanager.AppGraph
import com.attentionmanager.domain.model.AttentionResult

class DigestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return when (AppGraph.from(applicationContext).digestGeneratorUseCase.generateAndPostDigest()) {
            is AttentionResult.Success -> Result.success()
            is AttentionResult.Failure -> Result.retry()
        }
    }
}
