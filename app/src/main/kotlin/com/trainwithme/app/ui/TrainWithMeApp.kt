package com.trainwithme.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trainwithme.app.navigation.Routes
import com.trainwithme.app.widget.NextWorkoutWidgetProvider
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
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val context = LocalContext.current
        LaunchedEffect(currentRoute) {
            if (currentRoute == Routes.DASHBOARD) {
                val manager = AppWidgetManager.getInstance(context)
                val widgetIds = manager.getAppWidgetIds(
                    ComponentName(context, NextWorkoutWidgetProvider::class.java),
                )
                if (widgetIds.isNotEmpty()) {
                    context.sendBroadcast(
                        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                            .setComponent(ComponentName(context, NextWorkoutWidgetProvider::class.java))
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds),
                    )
                }
            }
        }
        val selectedTab = when (currentRoute) {
            Routes.PLAN, Routes.DAY_DETAIL_PATTERN -> Routes.PLAN
            Routes.PROGRESS -> Routes.PROGRESS
            else -> Routes.DASHBOARD
        }
        val showNavigation = currentRoute == null || currentRoute in setOf(
            Routes.DASHBOARD,
            Routes.PLAN,
            Routes.PROGRESS,
        )

        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showNavigation) {
                    AppDestinationBar(
                        selectedRoute = selectedTab,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                modifier = Modifier.padding(contentPadding),
                startDestination = Routes.DASHBOARD,
                enterTransition = {
                    fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 }
                },
                exitTransition = {
                    fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 18 }
                },
                popEnterTransition = {
                    fadeIn(tween(200)) + slideInHorizontally(tween(240)) { -it / 16 }
                },
                popExitTransition = {
                    fadeOut(tween(130)) + slideOutHorizontally(tween(170)) { it / 18 }
                },
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardRoute(
                        onStartWorkout = { dayId -> navController.navigate(Routes.workout(dayId)) },
                        onOpenPlan = { navController.navigate(Routes.PLAN) },
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

@Composable
internal fun AppDestinationBar(
    selectedRoute: String,
    onSelect: (String) -> Unit,
) {
    NavigationBar {
        listOf(
            AppTab(Routes.DASHBOARD, "Today", Icons.Default.Home),
            AppTab(Routes.PLAN, "Plan", Icons.Default.DateRange),
            AppTab(Routes.PROGRESS, "Progress", Icons.Default.Insights),
        ).forEach { tab ->
            NavigationBarItem(
                selected = selectedRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}

private data class AppTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
