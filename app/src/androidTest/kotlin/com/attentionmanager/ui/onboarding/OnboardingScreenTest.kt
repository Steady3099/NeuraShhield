package com.attentionmanager.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.attentionmanager.ui.theme.NeuraShhieldTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboardingFlowRendersRequiredPermissionsAndDisabledCta() {
        composeRule.setContent {
            NeuraShhieldTheme {
                OnboardingScreen(
                    permissionState = OnboardingPermissionState(
                        notificationListenerGranted = false,
                        postNotificationsGranted = false,
                        activityRecognitionGranted = false,
                        calendarGranted = false
                    ),
                    onRequestPostNotifications = {},
                    onOpenNotificationSettings = {},
                    onRefreshPermissionState = {},
                    onStartLearning = {}
                )
            }
        }

        composeRule.onNodeWithText("NeuraShhield").assertIsDisplayed()
        composeRule.onNodeWithTag("OnboardingPage0").assertIsDisplayed()
        composeRule.onNodeWithTag("PermissionRationale").assertIsDisplayed()
        composeRule.onNodeWithText("Notification access").assertIsDisplayed()
        composeRule.onNodeWithText("Digest notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Start Learning").assertIsNotEnabled()
    }
}
