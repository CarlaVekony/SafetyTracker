package com.example.safetytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.safetytracker.data.model.EmergencyAlert
import com.example.safetytracker.data.model.EmergencyContact

@Database(
    entities = [EmergencyContact::class, EmergencyAlert::class],
    version = 2,
    exportSchema = false
)
abstract class SafetyDatabase : RoomDatabase() {

    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun emergencyAlertDao(): EmergencyAlertDao

    companion object {
        @Volatile
        private var INSTANCE: SafetyDatabase? = null

        fun getDatabase(context: Context): SafetyDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafetyDatabase::class.java,
                    "safety_database"
                )
                    .fallbackToDestructiveMigration()   // 🔥 FIX: previne crash-urile Room
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
