package com.agon.app

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agon.app.ui.screens.dashboard.DashboardRoute
import com.agon.app.ui.screens.onboarding.OnboardingRoute
import com.agon.app.ui.screens.plan.DayDetailRoute
import com.agon.app.ui.screens.plan.PlanBrowserRoute
import com.agon.app.ui.screens.progress.ProgressRoute
import com.agon.app.ui.screens.settings.SettingsRoute
import com.agon.app.ui.screens.workout.WorkoutRoute
import com.agon.app.ui.theme.TrainWithMeColors
import com.agon.app.viewmodel.AppViewModelFactory

/**
 * All navigation routes in one place: features never know about each other, only this wires them.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val ONBOARDING = "onboarding"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val PROGRESS = "progress"
    const val WORKOUT_BASE = "workout"

    fun dayDetail(dayId: String) = "day/$dayId"
    fun workout(dayId: String) = "$WORKOUT_BASE/$dayId"

    const val DAY_DETAIL_PATTERN = "day/{dayId}"
    const val WORKOUT_PATTERN = "$WORKOUT_BASE/{dayId}"
}

@Composable
fun TrainWithMeNav(
    factory: AppViewModelFactory,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

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
                EmberBottomBar(
                    selectedRoute = selectedTab,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 12 }
            },
            exitTransition = {
                fadeOut(tween(150)) + slideOutHorizontally(tween(200)) { -it / 16 }
            },
            popEnterTransition = {
                fadeIn(tween(200)) + slideInHorizontally(tween(260)) { -it / 14 }
            },
            popExitTransition = {
                fadeOut(tween(140)) + slideOutHorizontally(tween(190)) { it / 16 }
            },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardRoute(
                    factory = factory,
                    onStartWorkout = { dayId -> navController.navigate(Routes.workout(dayId)) },
                    onOpenPlan = { navController.navigate(Routes.PLAN) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenOnboarding = { navController.navigate(Routes.ONBOARDING) },
                    onOpenDay = { dayId -> navController.navigate(Routes.dayDetail(dayId)) },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingRoute(
                    factory = factory,
                    onFinished = {
                        navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                    },
                    onCancelled = { navController.popBackStack() },
                )
            }
            composable(Routes.PLAN) {
                PlanBrowserRoute(
                    factory = factory,
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
                    factory = factory,
                    onBack = { navController.popBackStack() },
                    onStart = { navController.navigate(Routes.workout(dayId)) },
                )
            }
            composable(
                route = Routes.WORKOUT_PATTERN,
                arguments = listOf(navArgument("dayId") { defaultValue = "" }),
            ) {
                WorkoutRoute(
                    factory = factory,
                    onExit = { navController.popBackStack() },
                    onFinished = { navController.popBackStack(Routes.DASHBOARD, inclusive = false) },
                )
            }
            composable(Routes.PROGRESS) {
                ProgressRoute(factory = factory)
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(
                    factory = factory,
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

private data class EmberTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

/**
 * Floating pill bottom bar: blurred backdrop, ember glow behind the active tab,
 * springy icon scale and haptic ticks.
 */
@Composable
private fun EmberBottomBar(
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            EmberTab(Routes.DASHBOARD, "Today", Icons.Outlined.Home, Icons.Filled.Home),
            EmberTab(Routes.PLAN, "Plan", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
            EmberTab(Routes.PROGRESS, "Progress", Icons.Outlined.Insights, Icons.Filled.Insights),
        )
    }
    val view = LocalView.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            // soft glow under the bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(44.dp)
                    .blur(28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        CircleShape,
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val selected = selectedRoute == tab.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                        ),
                        label = "tab scale",
                    )
                    val glowAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(220),
                        label = "tab glow",
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                                }
                                onSelect(tab.route)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (glowAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f * glowAlpha),
                                                    Color.Transparent,
                                                ),
                                            ),
                                        ),
                                )
                            }
                            Icon(
                                imageVector = if (selected) tab.iconSelected else tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.scale(scale),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 18.dp else 0.dp,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMedium,
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                            ),
                            label = "tab dot",
                        )
                        if (dotWidth > 1.dp) {
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = dotWidth, height = 3.dp)
                                    .background(TrainWithMeColors.emberGradient, CircleShape),
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(10.dp),
        )
    }
}
