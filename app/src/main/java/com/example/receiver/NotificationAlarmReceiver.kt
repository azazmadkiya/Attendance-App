package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.HaazriDatabase
import com.example.data.HaazriRepository
import com.example.util.NotificationHelper
import com.example.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            NotificationScheduler.ACTION_DAILY_REMINDER -> {
                NotificationHelper.showNotification(
                    context = context,
                    title = "⏰ Daily Attendance Reminder",
                    message = "Good morning! Time to mark attendance and shifts for your workers in haazriPro.",
                    notificationId = NotificationScheduler.NOTIFICATION_ID_DAILY
                )

                // Reschedule for next day
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HaazriDatabase.getDatabase(context)
                        val repository = HaazriRepository(db)
                        val settings = repository.notificationSettings.firstOrNull()
                        if (settings?.dailyReminderEnabled == true) {
                            NotificationScheduler.scheduleDailyReminder(
                                context,
                                settings.reminderTime,
                                enabled = true
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            NotificationScheduler.ACTION_MISSED_CHECKOUT -> {
                NotificationHelper.showNotification(
                    context = context,
                    title = "⚠️ Evening Shift & Check-out Nudge",
                    message = "Please review today's attendance and verify all workers' check-outs and wage calculations.",
                    notificationId = NotificationScheduler.NOTIFICATION_ID_MISSED_CHECKOUT
                )

                // Reschedule for next day evening
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HaazriDatabase.getDatabase(context)
                        val repository = HaazriRepository(db)
                        val settings = repository.notificationSettings.firstOrNull()
                        if (settings?.missedCheckoutNudge == true) {
                            NotificationScheduler.scheduleMissedCheckout(context, enabled = true)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            NotificationScheduler.ACTION_WEEKLY_SUMMARY -> {
                NotificationHelper.showNotification(
                    context = context,
                    title = "📊 Weekly Payroll & Attendance Digest",
                    message = "Your weekly worker attendance summary and payout report is ready to review in haazriPro.",
                    notificationId = NotificationScheduler.NOTIFICATION_ID_WEEKLY
                )

                // Reschedule for next week
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HaazriDatabase.getDatabase(context)
                        val repository = HaazriRepository(db)
                        val settings = repository.notificationSettings.firstOrNull()
                        if (settings?.weeklyReportEnabled == true) {
                            NotificationScheduler.scheduleWeeklyReport(context, enabled = true)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
