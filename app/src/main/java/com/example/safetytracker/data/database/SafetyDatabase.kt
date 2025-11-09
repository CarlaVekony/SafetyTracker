package com.example.safetytracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.safetytracker.data.model.EmergencyContact

@Database(
    entities = [EmergencyContact::class],
    version = 1,
    exportSchema = true
)
abstract class SafetyDatabase : RoomDatabase() {
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        @Volatile
        private var INSTANCE: SafetyDatabase? = null

        fun getDatabase(context: Context): SafetyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafetyDatabase::class.java,
                    "safety_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
