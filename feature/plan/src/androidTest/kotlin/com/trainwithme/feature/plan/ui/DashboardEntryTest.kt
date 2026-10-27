package com.trainwithme.feature.plan.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.feature.plan.vm.DashboardUiState
import com.trainwithme.feature.plan.vm.TodayKind
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardEntryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noPlanDashboardOffersAnImmediatePlanAction() {
        var openedOnboarding = false
        composeRule.setContent {
            TrainWithMeTheme {
                DashboardContent(
                    state = DashboardUiState(isLoading = false, todayKind = TodayKind.NO_PLAN),
                    onStartWorkout = {},
                    onOpenPlan = {},
                    onOpenSettings = {},
                    onOpenOnboarding = { openedOnboarding = true },
                    onOpenDay = {},
                    onLogWeight = {},
                    onRebuildPlan = {},
                    onDismissMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("No active plan yet").assertIsDisplayed()
        composeRule.onNodeWithText("Build my plan").performClick()
        composeRule.runOnIdle { assertTrue(openedOnboarding) }
    }
}
