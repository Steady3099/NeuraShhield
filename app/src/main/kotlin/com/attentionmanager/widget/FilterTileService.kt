package com.attentionmanager.widget

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.attentionmanager.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FilterTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { updateTile(AppGraph.from(this@FilterTileService).attentionController.isEnabled.first()) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val enabled = AppGraph.from(this@FilterTileService).attentionController.toggle()
            updateTile(enabled)
        }
    }

    private fun updateTile(enabled: Boolean) {
        qsTile?.apply {
            label = if (enabled) "AI Filter: ON" else "AI Filter: OFF"
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
