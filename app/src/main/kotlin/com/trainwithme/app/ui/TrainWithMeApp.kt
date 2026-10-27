package com.trainwithme.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trainwithme.app.navigation.Routes
import com.trainwithme.core.data.repository.ThemeMode
import com.trainwithme.core.designsystem.theme.TrainWithMeTheme
import com.trainwithme.feature.onboarding.ui.OnboardingRoute
import com.trainwithme.feature.plan.ui.DashboardRoute
import com.trainwithme.feature.plan.ui.DayDetailRoute
import com.trainwithme.feature.plan.ui.PlanBrowserRoute
import com.trainwithme.feature.plan.ui.SettingsRoute
import com.trainwithme.feature.progress.ui.ProgressRoute
import com.trainwithme.feature.workout.ui.WorkoutRoute

/**
 * Root composable: owns the theme switch and the single NavHost. Features are entered through
 * their `*Route` composables; nothing else about them is known here.
 */
@Composable
fun TrainWithMeApp(
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()

    TrainWithMeTheme(
        darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        },
    ) {
        Surface(modifier = modifier) {
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(120)) },
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardRoute(
                        onStartWorkout = { dayId -> navController.navigate(Routes.workout(dayId)) },
                        onOpenPlan = { navController.navigate(Routes.PLAN) },
                        onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) },
                        onOpenDay = { dayId -> navController.navigate(Routes.dayDetail(dayId)) },
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingRoute(
                        onFinished = {
                            // Onboarding may be reached from the dashboard (no plan) or settings.
                            navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                        },
                        onCancelled = { navController.popBackStack() },
                    )
                }

                composable(Routes.PLAN) {
                    PlanBrowserRoute(
                        onBack = { navController.popBackStack() },
                        onOpenDay = { dayId -> navController.navigate(Routes.dayDetail(dayId)) },
                    )
                }

                composable(
                    route = Routes.DAY_DETAIL_PATTERN,
                    arguments = listOf(navArgument("dayId") { defaultValue = "" }),
                ) { entry ->
                    val dayId = entry.arguments?.getString("dayId").orEmpty()
                    DayDetailRoute(
                        onBack = { navController.popBackStack() },
                        onStart = { navController.navigate(Routes.workout(dayId)) },
                    )
                }

                composable(
                    route = Routes.WORKOUT_PATTERN,
                    arguments = listOf(navArgument("dayId") { defaultValue = "" }),
                ) {
                    WorkoutRoute(
                        onExit = { navController.popBackStack() },
                        onFinished = { navController.popBackStack(Routes.DASHBOARD, inclusive = false) },
                    )
                }

                composable(Routes.PROGRESS) {
                    ProgressRoute(onBack = { navController.popBackStack() })
                }

                composable(Routes.SETTINGS) {
                    SettingsRoute(
                        onBack = { navController.popBackStack() },
                        onEditProfile = { navController.navigate(Routes.ONBOARDING) },
                        onDataDeleted = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
