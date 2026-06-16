package com.attentionmanager.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.attentionmanager.data.database.NotificationEntity
import com.attentionmanager.domain.model.PriorityTier
import com.attentionmanager.ui.components.NotificationActionSheet

@Composable
fun ManagedFeedScreen(viewModel: ManagedFeedViewModel, modifier: Modifier = Modifier) {
    val notifications = viewModel.notifications.collectAsLazyPagingItems()
    var selected by remember { mutableStateOf<NotificationEntity?>(null) }
    var pendingPriorityAction by remember { mutableStateOf<PendingPriorityAction?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Managed Feed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Notifications classified by NeuraShhield will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            notifications.loadState.refresh is LoadState.Loading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            notifications.loadState.refresh is LoadState.Error -> {
                val error = notifications.loadState.refresh as LoadState.Error
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = "Couldn’t load feed",
                    body = error.error.message ?: "Something went wrong while loading managed notifications.",
                    modifier = Modifier.fillMaxSize()
                )
            }
            notifications.itemCount == 0 -> {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = "No managed notifications yet",
                    body = "After you grant notification access, incoming notifications will be classified and listed here.",
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications.itemCount) { index ->
                        notifications[index]?.let { notification ->
                            NotificationFeedRow(
                                notification = notification,
                                onOpenActions = { selected = notification }
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { notification ->
        NotificationActionSheet(
            notification = notification,
            onAlwaysUrgent = {
                pendingPriorityAction = PendingPriorityAction.Promote(notification)
                selected = null
            },
            onAlwaysLow = {
                pendingPriorityAction = PendingPriorityAction.Demote(notification)
                selected = null
            },
            onDismissDontLearn = {
                viewModel.dismissDontLearn(notification)
                selected = null
            },
            onReportSpam = {
                viewModel.reportSpam(notification)
                selected = null
            },
            onDismissRequest = { selected = null }
        )
    }

    pendingPriorityAction?.let { action ->
        PriorityChangeDialog(
            action = action,
            onConfirm = {
                when (action) {
                    is PendingPriorityAction.Promote -> viewModel.promote(action.notification)
                    is PendingPriorityAction.Demote -> viewModel.demote(action.notification)
                }
                pendingPriorityAction = null
            },
            onDismiss = { pendingPriorityAction = null }
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationFeedRow(
    notification: NotificationEntity,
    onOpenActions: () -> Unit
) {
    NotificationRow(
        notification = notification,
        onOpenActions = onOpenActions,
        modifier = Modifier.combinedClickable(
            onClick = {},
            onLongClick = onOpenActions
        )
    )
}

@Composable
private fun NotificationRow(
    notification: NotificationEntity,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.sender ?: notification.packageName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.priorityTier.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = when (notification.priorityTier) {
                            PriorityTier.URGENT -> MaterialTheme.colorScheme.error
                            PriorityTier.IMPORTANT -> MaterialTheme.colorScheme.primary
                            PriorityTier.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    IconButton(
                        onClick = onOpenActions,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More actions"
                        )
                    }
                }
            }
            Text(
                text = notification.title.ifBlank { "(untitled)" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriorityChangeDialog(
    action: PendingPriorityAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val notification = action.notification
    val target = when (action) {
        is PendingPriorityAction.Promote -> "Always urgent"
        is PendingPriorityAction.Demote -> "Always low"
    }
    val subject = when (action) {
        is PendingPriorityAction.Promote -> notification.sender ?: notification.title.ifBlank { notification.packageName }
        is PendingPriorityAction.Demote -> notification.packageName
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target) },
        text = { Text(subject) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private sealed class PendingPriorityAction(open val notification: NotificationEntity) {
    data class Promote(override val notification: NotificationEntity) : PendingPriorityAction(notification)
    data class Demote(override val notification: NotificationEntity) : PendingPriorityAction(notification)
}

private val PriorityTier.label: String
    get() = when (this) {
        PriorityTier.URGENT -> "Urgent"
        PriorityTier.IMPORTANT -> "Important"
        PriorityTier.LOW -> "Low"
    }
