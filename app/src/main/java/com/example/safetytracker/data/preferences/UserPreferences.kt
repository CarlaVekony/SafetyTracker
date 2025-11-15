package com.example.safetytracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safety_tracker_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        private val AUTO_MONITORING_KEY = booleanPreferencesKey("auto_monitoring")
        
        @Volatile
        private var INSTANCE: UserPreferences? = null
        
        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }
    
    val isDynamicColorsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS_KEY] ?: true
    }
    
    val isAutoMonitoringEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_MONITORING_KEY] ?: false
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_KEY] = enabled
        }
    }
    
    suspend fun setAutoMonitoring(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_MONITORING_KEY] = enabled
        }
    }
}