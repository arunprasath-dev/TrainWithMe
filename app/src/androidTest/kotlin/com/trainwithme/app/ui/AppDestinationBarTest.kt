package com.trainwithme.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainwithme.app.navigation.Routes
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDestinationBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun destinationsAreVisibleAndSelectionNavigates() {
        var selectedRoute = Routes.DASHBOARD
        composeRule.setContent {
            TrainWithMeTheme {
                AppDestinationBar(
                    selectedRoute = selectedRoute,
                    onSelect = { selectedRoute = it },
                )
            }
        }

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Plan").assertIsDisplayed()
        composeRule.onNodeWithText("Progress").assertIsDisplayed()
        composeRule.onNodeWithText("Today").assertIsSelected()
        composeRule.onNodeWithText("Plan").performClick()
        composeRule.runOnIdle { assertEquals(Routes.PLAN, selectedRoute) }
    }
}
