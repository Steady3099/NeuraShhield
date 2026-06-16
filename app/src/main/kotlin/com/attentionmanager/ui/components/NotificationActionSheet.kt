package com.attentionmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.attentionmanager.data.database.NotificationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationActionSheet(
    notification: NotificationEntity,
    onAlwaysUrgent: () -> Unit,
    onAlwaysLow: () -> Unit,
    onDismissDontLearn: () -> Unit,
    onReportSpam: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            ListItem(
                headlineContent = { Text("Always Urgent from ${notification.sender ?: notification.title}") },
                leadingContent = { Icon(Icons.Filled.PriorityHigh, contentDescription = null) },
                modifier = Modifier.clickableNoRipple(onAlwaysUrgent)
            )
            ListItem(
                headlineContent = { Text("Always Low from ${notification.packageName}") },
                leadingContent = { Icon(Icons.Filled.Block, contentDescription = null) },
                modifier = Modifier.clickableNoRipple(onAlwaysLow)
            )
            ListItem(
                headlineContent = { Text("Dismiss + don't learn") },
                leadingContent = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                modifier = Modifier.clickableNoRipple(onDismissDontLearn)
            )
            ListItem(
                headlineContent = { Text("Report as spam") },
                leadingContent = { Icon(Icons.Filled.Report, contentDescription = null) },
                modifier = Modifier.clickableNoRipple(onReportSpam)
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    clickable(onClick = onClick)
