package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.NotificationSetting
import com.example.receiver.NotificationAlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {
    const val ACTION_DAILY_REMINDER = "com.example.haazri.ACTION_DAILY_REMINDER"
    const val ACTION_MISSED_CHECKOUT = "com.example.haazri.ACTION_MISSED_CHECKOUT"
    const val ACTION_WEEKLY_SUMMARY = "com.example.haazri.ACTION_WEEKLY_SUMMARY"

    const val NOTIFICATION_ID_DAILY = 1001
    const val NOTIFICATION_ID_MISSED_CHECKOUT = 1002
    const val NOTIFICATION_ID_WEEKLY = 1003

    private const val REQUEST_CODE_DAILY = 2001
    private const val REQUEST_CODE_MISSED_CHECKOUT = 2002
    private const val REQUEST_CODE_WEEKLY = 2003

    fun scheduleAll(context: Context, settings: NotificationSetting) {
        scheduleDailyReminder(context, settings.reminderTime, settings.dailyReminderEnabled)
        scheduleMissedCheckout(context, settings.missedCheckoutNudge)
        scheduleWeeklyReport(context, settings.weeklyReportEnabled)
    }

    fun scheduleDailyReminder(context: Context, timeString: String, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val targetTime = parseTimeStringToCalendar(timeString)
        setAlarmCompat(alarmManager, targetTime.timeInMillis, pendingIntent)
    }

    fun scheduleMissedCheckout(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_MISSED_CHECKOUT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MISSED_CHECKOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        // Default 07:00 PM (19:00)
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        setAlarmCompat(alarmManager, targetCalendar.timeInMillis, pendingIntent)
    }

    fun scheduleWeeklyReport(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_WEEKLY_SUMMARY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WEEKLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        // Saturday 06:00 PM (18:00)
        val targetCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        setAlarmCompat(alarmManager, targetCalendar.timeInMillis, pendingIntent)
    }

    private fun setAlarmCompat(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun parseTimeStringToCalendar(timeString: String): Calendar {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()

        var parsed = false
        val formats = arrayOf(
            SimpleDateFormat("hh:mm a", Locale.US),
            SimpleDateFormat("h:mm a", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US),
            SimpleDateFormat("H:mm", Locale.US)
        )

        for (sdf in formats) {
            try {
                val date = sdf.parse(timeString.trim())
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    target.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                    target.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                    target.set(Calendar.SECOND, 0)
                    target.set(Calendar.MILLISECOND, 0)
                    parsed = true
                    break
                }
            } catch (_: Exception) {
            }
        }

        if (!parsed) {
            // Default to 09:00 AM
            target.set(Calendar.HOUR_OF_DAY, 9)
            target.set(Calendar.MINUTE, 0)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
        }

        // If time has passed today, schedule for tomorrow
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target
    }
}
