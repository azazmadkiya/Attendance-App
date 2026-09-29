package com.attendance.app.azaz

import android.app.Application
import com.google.firebase.FirebaseApp

class HaazriApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
