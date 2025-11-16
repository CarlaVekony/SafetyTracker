package com.example.safetytracker.data.database

import androidx.room.*
import com.example.safetytracker.data.model.EmergencyAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyAlertDao {

    // ---------------------------------------------------------
    // READ
    // ---------------------------------------------------------

    @Query("SELECT * FROM emergency_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<EmergencyAlert>>

    @Query("SELECT * FROM emergency_alerts WHERE id = :id LIMIT 1")
    fun getAlertById(id: Long): Flow<EmergencyAlert?>

    @Query("SELECT * FROM emergency_alerts WHERE status = :status ORDER BY timestamp DESC")
    fun getAlertsByStatus(status: EmergencyAlert.AlertStatus): Flow<List<EmergencyAlert>>

    @Query("SELECT COUNT(*) FROM emergency_alerts")
    suspend fun getAlertCount(): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: EmergencyAlert): Long

    @Update
    suspend fun updateAlert(alert: EmergencyAlert)

    @Delete
    suspend fun deleteAlert(alert: EmergencyAlert)

    @Query("DELETE FROM emergency_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM emergency_alerts")
    suspend fun deleteAll()
}
