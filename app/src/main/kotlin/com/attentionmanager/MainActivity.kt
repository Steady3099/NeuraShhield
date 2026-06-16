package com.attentionmanager

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attentionmanager.core.PermissionUtils
import com.attentionmanager.core.viewModelFactory
import com.attentionmanager.ui.digest.DigestScreen
import com.attentionmanager.ui.digest.DigestViewModel
import com.attentionmanager.ui.feed.ManagedFeedScreen
import com.attentionmanager.ui.feed.ManagedFeedViewModel
import com.attentionmanager.ui.home.HomeScreen
import com.attentionmanager.ui.home.HomeViewModel
import com.attentionmanager.ui.onboarding.OnboardingPermissionState
import com.attentionmanager.ui.onboarding.OnboardingScreen
import com.attentionmanager.ui.settings.SettingsScreen
import com.attentionmanager.ui.settings.SettingsViewModel
import com.attentionmanager.ui.theme.NeuraShhieldTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = AppGraph.from(this)
        setContent {
            NeuraShhieldTheme {
                val preferences by graph.preferenceRepository.preferences.collectAsStateWithLifecycle(
                    initialValue = com.attentionmanager.data.preferences.FilterPreferences.getDefaultInstance()
                )
                var permissionState by remember { mutableStateOf(permissionSnapshot()) }
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    permissionState = permissionSnapshot()
                }
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionState = permissionSnapshot()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(permissionState.requiredGranted) {
                    graph.preferenceRepository.setNotificationPermissionGranted(permissionState.requiredGranted)
                }

                if (!preferences.onboardingCompleted) {
                    OnboardingScreen(
                        permissionState = permissionState,
                        onRequestPostNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                permissionState = permissionSnapshot()
                            }
                        },
                        onOpenNotificationSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onRefreshPermissionState = {
                            permissionState = permissionSnapshot()
                        },
                        onStartLearning = {
                            scope.launch {
                                graph.preferenceRepository.setOnboardingCompleted(true)
                            }
                        }
                    )
                } else {
                    MainAppScaffold(graph)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppGraph.from(this).contextSignalProvider.start()
    }

    private fun permissionSnapshot(): OnboardingPermissionState =
        OnboardingPermissionState(
            notificationListenerGranted = PermissionUtils.hasNotificationListenerAccess(this),
            postNotificationsGranted = PermissionUtils.canPostNotifications(this),
            activityRecognitionGranted = PermissionUtils.hasActivityRecognition(this),
            calendarGranted = PermissionUtils.hasCalendarAccess(this)
        )
}

private enum class MainDestination(val label: String) {
    Home("Home"),
    Feed("Feed"),
    Digest("Digest"),
    Settings("Settings")
}

@Composable
private fun MainAppScaffold(graph: AppGraph) {
    var destination by remember { mutableStateOf(MainDestination.Home) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    MainDestination.Home -> Icons.Filled.Home
                                    MainDestination.Feed -> Icons.Filled.Notifications
                                    MainDestination.Digest -> Icons.Filled.Summarize
                                    MainDestination.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (destination) {
            MainDestination.Home -> {
                val viewModel: HomeViewModel = viewModel(
                    factory = viewModelFactory {
                        HomeViewModel(
                            graph.notificationRepository,
                            graph.digestRepository,
                            graph.preferenceRepository
                        )
                    }
                )
                HomeScreen(viewModel = viewModel, modifier = modifier)
            }
            MainDestination.Feed -> {
                val viewModel: ManagedFeedViewModel = viewModel(
                    factory = viewModelFactory {
                        ManagedFeedViewModel(
                            graph.notificationRepository,
                            graph.userFeedbackUseCase
                        )
                    }
                )
                ManagedFeedScreen(viewModel = viewModel, modifier = modifier)
            }
            MainDestination.Digest -> {
                val viewModel: DigestViewModel = viewModel(
                    factory = viewModelFactory { DigestViewModel(graph.digestRepository) }
                )
                DigestScreen(viewModel = viewModel, modifier = modifier)
            }
            MainDestination.Settings -> {
                val viewModel: SettingsViewModel = viewModel(
                    factory = viewModelFactory {
                        SettingsViewModel(
                            graph.preferenceRepository,
                            graph.contactPriorityRepository,
                            graph.spamLogRepository,
                            graph.digestScheduler,
                            graph.userFeedbackUseCase
                        )
                    }
                )
                SettingsScreen(viewModel = viewModel, modifier = modifier)
            }
        }
    }
}
