package com.agon.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.agon.app.R
import com.agon.app.TrainWithMeApp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Schedules (or cancels) the local training-day reminder. */
interface ReminderScheduler {
    suspend fun reschedule(enabled: Boolean, hour: Int, minute: Int)
    fun cancel()
}

class DefaultReminderScheduler(
    context: Context,
) : ReminderScheduler {

    private val appContext = context.applicationContext

    override suspend fun reschedule(enabled: Boolean, hour: Int, minute: Int) {
        if (!enabled) {
            cancel()
            return
        }
        val zone = (appContext as TrainWithMeApp).timeProvider.zone()
        val now = LocalDateTime.now(zone)
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = java.time.Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            // REPLACE keeps a single reminder alive when the user changes the time.
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
        Log.i("Reminders", "Reminder scheduled in $delayMinutes minutes")
    }

    override fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_NAME)
    }

    companion object {
        const val UNIQUE_NAME = "workout_reminder"
        const val TAG = "reminders"
    }
}

/**
 * Fires a local notification on training days when the session has not been completed. It reads
 * the plan, so it stays correct when the programme is regenerated.
 */
class WorkoutReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as TrainWithMeApp
        val settings = app.settingsRepository.current()
        if (!settings.remindersEnabled) return Result.success()

        val today = app.timeProvider.today()
        val plan = app.planRepository.activePlan() ?: return Result.success()
        val day = plan.dayOn(today) ?: return Result.success()
        if (day.isRestDay) return Result.success()
        if (app.workoutRepository.hasCompletedSession(day.id, today)) return Result.success()

        showNotification(day.title, day.exerciseCount, day.estimatedMinutes)
        return Result.success()
    }

    private fun showNotification(title: String, exercises: Int, minutes: Int) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Today: $title")
            .setContentText("$exercises exercises · about $minutes minutes. Your plan is ready.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { pending?.let(::setContentIntent) }
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Workout reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Reminds you on training days" },
        )
    }

    companion object {
        const val CHANNEL_ID = "workout_reminders"
        const val NOTIFICATION_ID = 1001
    }
}
