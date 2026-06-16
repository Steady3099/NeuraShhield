package com.attentionmanager

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AttentionManagerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val graph = AppGraph.from(this)
        graph.contextSignalProvider.start()
        applicationScope.launch {
            graph.digestScheduler.syncDigestSchedule(
                graph.preferenceRepository.preferences.first().digestIntervalHours
            )
        }
    }
}
