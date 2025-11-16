package com.example.safetytracker.data.repository

import android.content.Context
import android.util.Log
import com.example.safetytracker.data.database.SafetyDatabase
import com.example.safetytracker.data.model.EmergencyAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class EmergencyAlertRepository(context: Context) {

    private val database = SafetyDatabase.getDatabase(context)
    private val alertDao = database.emergencyAlertDao()

    companion object {
        private const val TAG = "EmergencyAlertRepo"

        @Volatile
        private var INSTANCE: EmergencyAlertRepository? = null

        fun getInstance(context: Context): EmergencyAlertRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EmergencyAlertRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // ---------------------------------------------------------
    // GET ALL ALERTS (FLOW)
    // ---------------------------------------------------------
    fun getAllAlerts(): Flow<List<EmergencyAlert>> {
        Log.d(TAG, "Fetching all emergency alerts")
        return alertDao.getAllAlerts().map { alerts ->
            Log.d(TAG, "Retrieved ${alerts.size} emergency alerts")
            alerts
        }
    }

    // ---------------------------------------------------------
    // GET ALERT BY ID (FLOW)
    // ---------------------------------------------------------
    fun getAlertById(id: Long): Flow<EmergencyAlert?> {
        Log.d(TAG, "Fetching alert by ID: $id")
        return alertDao.getAlertById(id).map { alert ->
            if (alert != null) {
                Log.d(TAG, "Found alert ID=$id")
            } else {
                Log.d(TAG, "No alert found with ID=$id")
            }
            alert
        }
    }

    // ---------------------------------------------------------
    // GET BY STATUS
    // ---------------------------------------------------------
    fun getAlertsByStatus(status: EmergencyAlert.AlertStatus): Flow<List<EmergencyAlert>> {
        Log.d(TAG, "Fetching alerts with status: $status")
        return alertDao.getAlertsByStatus(status).map { alerts ->
            Log.d(TAG, "Found ${alerts.size} alerts with status $status")
            alerts
        }
    }

    // ---------------------------------------------------------
    // INSERT ALERT
    // ---------------------------------------------------------
    suspend fun insertAlert(alert: EmergencyAlert): Long {
        Log.d(TAG, "Inserting new alert => Reason=${alert.detectionReason}")
        return try {
            val id = alertDao.insertAlert(alert)
            Log.d(TAG, "Successfully inserted alert ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert alert: ${e.message}")
            throw e
        }
    }

    // ---------------------------------------------------------
    // UPDATE ALERT
    // ---------------------------------------------------------
    suspend fun updateAlert(alert: EmergencyAlert) {
        Log.d(TAG, "Updating alert ID=${alert.id} with status=${alert.status}")
        try {
            alertDao.updateAlert(alert)
            Log.d(TAG, "Successfully updated alert ID=${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update alert ${alert.id}: ${e.message}")
            throw e
        }
    }

    // ---------------------------------------------------------
    // DELETE ALERT
    // ---------------------------------------------------------
    suspend fun deleteAlert(alert: EmergencyAlert) {
        Log.d(TAG, "Deleting alert ID=${alert.id}")
        try {
            alertDao.deleteAlert(alert)
            Log.d(TAG, "Successfully deleted alert ID=${alert.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete alert ID ${alert.id}: ${e.message}")
            throw e
        }
    }

    // ---------------------------------------------------------
    // DELETE BY ID
    // ---------------------------------------------------------
    suspend fun deleteAlertById(id: Long) {
        Log.d(TAG, "Deleting alert by ID: $id")
        try {
            alertDao.deleteAlertById(id)
            Log.d(TAG, "Successfully deleted alert ID: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete alert ID $id: ${e.message}")
            throw e
        }
    }

    // ---------------------------------------------------------
    // DELETE ALL ALERTS
    // ---------------------------------------------------------
    suspend fun deleteAll() {
        Log.d(TAG, "Deleting ALL alerts")
        try {
            alertDao.deleteAll()
            Log.d(TAG, "Successfully deleted all alerts")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all alerts: ${e.message}")
            throw e
        }
    }

    // ---------------------------------------------------------
    // GET COUNT
    // ---------------------------------------------------------
    suspend fun getAlertCount(): Int {
        Log.d(TAG, "Fetching alert count")
        return try {
            val count = alertDao.getAlertCount()
            Log.d(TAG, "Alert count: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get alert count: ${e.message}")
            0
        }
    }

    // ---------------------------------------------------------
    // GET COUNT BY STATUS
    // ---------------------------------------------------------
    suspend fun getAlertCountByStatus(status: EmergencyAlert.AlertStatus): Int {
        Log.d(TAG, "Fetching alert count for status: $status")
        return try {
            val alerts = alertDao.getAlertsByStatus(status).first()
            val count = alerts.size
            Log.d(TAG, "Count for $status => $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get status count: ${e.message}")
            0
        }
    }
}
