package com.attentionmanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.attentionmanager.AppGraph
import com.attentionmanager.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FilterToggleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_TOGGLE) {
            val pending = goAsync()
            scope.launch {
                AppGraph.from(context).attentionController.toggle()
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    android.content.ComponentName(context, FilterToggleWidgetProvider::class.java)
                )
                updateWidgets(context, manager, ids, AppGraph.from(context).attentionController.isEnabled.first())
                pending.finish()
            }
        }
    }

    private fun updateAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
        scope.launch {
            val enabled = AppGraph.from(context).attentionController.isEnabled.first()
            updateWidgets(context, manager, ids, enabled)
        }
    }

    private fun updateWidgets(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        enabled: Boolean
    ) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_filter_toggle).apply {
                setTextViewText(
                    R.id.widget_toggle_text,
                    context.getString(if (enabled) R.string.widget_filter_on else R.string.widget_filter_off)
                )
                setOnClickPendingIntent(R.id.widget_toggle_text, togglePendingIntent(context))
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun togglePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            70,
            Intent(context, FilterToggleWidgetProvider::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        const val ACTION_TOGGLE = "com.attentionmanager.widget.TOGGLE_FILTER"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
