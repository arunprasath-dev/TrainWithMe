package com.trainwithme.app.navigation

import android.net.Uri
import com.trainwithme.feature.workout.vm.WorkoutViewModel

/**
 * All navigation routes in one place: features never know about each other, only :app wires them.
 * Arguments are encoded explicitly so day ids (which contain only [a-z0-9]) stay stable.
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val ONBOARDING = "onboarding"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val PROGRESS = "progress"
    const val WORKOUT_BASE = "workout"

    fun dayDetail(dayId: String) = "day/${Uri.encode(dayId)}"

    fun workout(dayId: String) = "$WORKOUT_BASE/${Uri.encode(dayId)}"

    const val DAY_DETAIL_PATTERN = "day/{dayId}"
    const val WORKOUT_PATTERN = "$WORKOUT_BASE/{${WorkoutViewModel.ARG_DAY_ID}}"

    fun dayIdArgument(pattern: String): String = pattern.substringAfter('/')
}
