package com.trainwithme.core.designsystem.component

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.core.model.progress.WeightPoint
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryActionIsReachableAndRespondsToTap() {
        var clicked = false
        composeRule.setContent {
            TrainWithMeTheme {
                PrimaryButton(text = "Start workout", onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("Start workout").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun weightChartExposesAnAccessibleSummary() {
        composeRule.setContent {
            TrainWithMeTheme {
                TrendLineChart(
                    points = listOf(
                        WeightPoint(LocalDate.of(2026, 10, 1), 80.0),
                        WeightPoint(LocalDate.of(2026, 10, 27), 78.5),
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription(
            "Weight trend, 2 measurements. From 80 kg on 1 Oct to 78.5 kg on 27 Oct; " +
                "overall down by 1.5 kg.",
        ).assertExists()
    }
}
