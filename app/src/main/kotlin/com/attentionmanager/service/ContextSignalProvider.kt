package com.attentionmanager.service

import android.Manifest
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.attentionmanager.domain.model.ActivityType
import com.attentionmanager.domain.model.AppContext
import com.google.android.gms.location.ActivityRecognition
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class ContextSignalProvider(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentContext: Flow<AppContext> = tickerFlow()
        .mapLatest {
            AppContext(
                activityType = ContextSignalStore.activityType.value,
                hasMeetingNext60Min = hasMeetingNext60Minutes(),
                currentHour = currentHour()
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppContext(currentHour = currentHour())
        )

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            41,
            Intent(context, ActivityRecognitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        ActivityRecognition.getClient(context)
            .requestActivityUpdates(30_000L, pendingIntent)
            .addOnFailureListener { Log.w(TAG, "Activity recognition subscription failed.", it) }
    }

    private suspend fun hasMeetingNext60Minutes(): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext false
        }
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + 60 * 60 * 1000)
        }.build()
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.END),
                "${CalendarContract.Instances.VISIBLE} = 1",
                null,
                null
            )?.use { cursor -> cursor.moveToFirst() } == true
        }.getOrDefault(false)
    }

    private fun tickerFlow() = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0

    private fun currentHour(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    companion object {
        private const val TAG = "ContextSignalProvider"
    }
}

object ContextSignalStore {
    val activityType = MutableStateFlow(ActivityType.UNKNOWN)
}
