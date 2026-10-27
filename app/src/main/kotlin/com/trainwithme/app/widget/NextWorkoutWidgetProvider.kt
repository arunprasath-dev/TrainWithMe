package com.trainwithme.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trainwithme.app.MainActivity
import com.trainwithme.app.R
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.WorkoutRepository
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.training.SessionStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Offline home-screen shortcut for the next planned session. No body metrics are exposed. */
@AndroidEntryPoint
class NextWorkoutWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var planRepository: PlanRepository
    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var timeProvider: TimeProvider

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val plan = planRepository.activePlan()
                val today = timeProvider.today()
                val todayPlanDay = plan?.dayOn(today)
                val nextDay = todayPlanDay?.takeUnless { it.isRestDay }
                    ?: plan?.nextTrainingDayOnOrAfter(today)
                val completedSessions = workoutRepository.observeCompletedSessions().first()
                val alreadyDone = nextDay != null && completedSessions.any {
                    it.dayId == nextDay.id && it.date == nextDay.date && it.status == SessionStatus.COMPLETED
                }
                val title = when {
                    plan == null -> context.getString(R.string.widget_no_plan)
                    nextDay == null -> context.getString(R.string.widget_no_upcoming_session)
                    else -> nextDay.title
                }
                val detail = when {
                    plan == null -> context.getString(R.string.widget_create_plan)
                    alreadyDone -> context.getString(R.string.widget_session_done)
                    nextDay == null -> context.getString(R.string.widget_recovery)
                    else -> context.getString(
                        R.string.widget_session_detail,
                        nextDay.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
                        nextDay.exerciseCount,
                        nextDay.estimatedMinutes,
                    )
                }
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openApp = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                appWidgetIds.forEach { widgetId ->
                    val views = RemoteViews(context.packageName, R.layout.widget_next_workout).apply {
                        setTextViewText(R.id.widget_title, title)
                        setTextViewText(R.id.widget_detail, detail)
                        setTextViewText(
                            R.id.widget_action,
                            context.getString(R.string.widget_open_app),
                        )
                        setOnClickPendingIntent(R.id.widget_root, openApp)
                        setOnClickPendingIntent(R.id.widget_action, openApp)
                        setContentDescription(R.id.widget_root, "$title. $detail")
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (_: Exception) {
                // Keep the last successful snapshot and let the next launch/periodic refresh retry.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
