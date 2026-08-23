package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Worker::class,
        AttendanceRecord::class,
        CashbookEntry::class,
        GeofenceConfig::class,
        NotificationSetting::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HaazriDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: HaazriDatabase? = null

        fun getDatabase(context: Context): HaazriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HaazriDatabase::class.java,
                    "haazri_pro_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
