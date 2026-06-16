package com.attentionmanager.ui.onboarding

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OnboardingPermissionState(
    val notificationListenerGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val activityRecognitionGranted: Boolean,
    val calendarGranted: Boolean
) {
    val requiredGranted: Boolean
        get() = notificationListenerGranted && postNotificationsGranted
}

@Composable
fun OnboardingScreen(
    permissionState: OnboardingPermissionState,
    onRequestPostNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRefreshPermissionState: () -> Unit,
    onStartLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                BrandIntro(permissionState.requiredGranted)
                PrivacyIntro(modifier = Modifier.testTag("OnboardingPage0"))
                SetupSection(
                    permissionState = permissionState,
                    onRequestPostNotifications = onRequestPostNotifications,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onRefreshPermissionState = onRefreshPermissionState
                )
                Text(
                    text = "Activity and calendar access are optional. You can enable them later from Settings for smarter context.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = onStartLearning,
                    enabled = permissionState.requiredGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp)
                        .height(52.dp)
                        .testTag("StartLearningButton")
                ) {
                    Text("Start Learning")
                }
            }
        }
    }
}

@Composable
private fun BrandIntro(requiredGranted: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }
            Column {
                Text(
                    text = "NeuraShhield",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Private notification intelligence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (requiredGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (requiredGranted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        ) {
            Text(
                text = if (requiredGranted) "Ready to start" else "Two permissions needed",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PrivacyIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Quiet the noise. Keep the urgent.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "NeuraShhield reads notifications on this device, classifies them locally, and creates hourly digests. Notification text is never sent to a server.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("PermissionRationale")
        )
    }
}

@Composable
private fun SetupSection(
    permissionState: OnboardingPermissionState,
    onRequestPostNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRefreshPermissionState: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Setup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        PermissionRow(
            title = "Notification access",
            body = "Required to classify incoming notifications.",
            granted = permissionState.notificationListenerGranted,
            actionLabel = if (permissionState.notificationListenerGranted) "Refresh" else "Open settings",
            onAction = {
                if (permissionState.notificationListenerGranted) {
                    onRefreshPermissionState()
                } else {
                    onOpenNotificationSettings()
                }
            }
        )
        PermissionRow(
            title = "Digest notifications",
            body = "Required to show summaries and OTP helper alerts.",
            granted = permissionState.postNotificationsGranted,
            actionLabel = if (permissionState.postNotificationsGranted) "Done" else "Allow",
            onAction = {
                if (!permissionState.postNotificationsGranted) onRequestPostNotifications()
            }
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    body: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (granted) "Granted" else "Missing",
                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!granted || actionLabel == "Refresh") {
                FilledTonalButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
