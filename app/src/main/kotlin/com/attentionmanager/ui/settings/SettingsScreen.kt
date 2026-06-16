package com.attentionmanager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var digestIntervalText by remember { mutableStateOf(state.digestIntervalText) }
    var rulesText by remember { mutableStateOf(state.priorityRulesText) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.digestIntervalText) { digestIntervalText = state.digestIntervalText }
    LaunchedEffect(state.priorityRulesText) { rulesText = state.priorityRulesText }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            ListItem(
                headlineContent = { Text("AI Filter") },
                supportingContent = { Text(if (state.filterEnabled) "Enabled" else "Disabled") },
                trailingContent = {
                    Switch(
                        checked = state.filterEnabled,
                        onCheckedChange = viewModel::setFilterEnabled
                    )
                }
            )
        }
        item { HorizontalDivider() }
        item {
            SettingsSection(title = "Digest Interval") {
                OutlinedTextField(
                    value = digestIntervalText,
                    onValueChange = { value ->
                        digestIntervalText = value.filter(Char::isDigit).take(2)
                    },
                    label = { Text("Every N hours") },
                    supportingText = { Text("Default is every hour. Enter 1-24 to make digests less frequent.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.saveDigestInterval(digestIntervalText) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save interval")
                }
            }
        }
        item {
            SettingsSection(title = "Priority Rules") {
                OutlinedTextField(
                    value = rulesText,
                    onValueChange = {
                        rulesText = it
                        viewModel.updateRulesText(it)
                    },
                    label = { Text("Rules") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            SettingsSection(title = "Contact Priorities") {
                if (state.contacts.isEmpty()) {
                    Text(
                        text = "No overrides yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.contacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(contact.displayName)
                            Text("%.2f".format(contact.priorityBoost))
                        }
                    }
                }
            }
        }
        item {
            SettingsSection(title = "Spam Log") {
                if (state.spamLog.isEmpty()) {
                    Text(
                        text = "No spam hidden",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(state.spamLog, key = { it.id }) { log ->
            ListItem(
                headlineContent = { Text(log.title.ifBlank { log.packageName }) },
                supportingContent = { Text("${log.score} • ${log.reasons.joinToString()}") }
            )
        }
        item {
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(
                    text = "Reset AI Model",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset local learning?") },
            text = {
                Text(
                    "This clears contact/app priority overrides, spam audit entries, and user promote/demote learning flags. Notification history and the trained base model stay intact."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAiModel()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}
