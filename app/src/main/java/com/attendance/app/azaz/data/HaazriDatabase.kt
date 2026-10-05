package com.attendance.app.azaz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Worker::class,
        AttendanceRecord::class,
        CashbookEntry::class,
        NotificationSetting::class
    ],
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workers ADD COLUMN monthlyWageBasis TEXT NOT NULL DEFAULT 'Fixed 30 Days'")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE workers ADD COLUMN monthlyWageBasis TEXT NOT NULL DEFAULT 'Fixed 30 Days'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): HaazriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HaazriDatabase::class.java,
                    "haazri_pro_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_1_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
