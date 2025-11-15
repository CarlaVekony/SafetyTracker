package com.example.safetytracker.data.database

import androidx.room.*
import com.example.safetytracker.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts")
    fun getAllContacts(): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): EmergencyContact?

    @Insert
    suspend fun insertContact(contact: EmergencyContact): Long

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    @Query("SELECT * FROM emergency_contacts WHERE isPrimary = 1")
    fun getPrimaryContact(): Flow<EmergencyContact?>
}
