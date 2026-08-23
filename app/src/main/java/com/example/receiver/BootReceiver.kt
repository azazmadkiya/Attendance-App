package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.HaazriDatabase
import com.example.data.HaazriRepository
import com.example.util.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HaazriDatabase.getDatabase(context)
                    val repository = HaazriRepository(db)
                    val settings = repository.notificationSettings.firstOrNull()
                    if (settings != null) {
                        NotificationScheduler.scheduleAll(context, settings)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
