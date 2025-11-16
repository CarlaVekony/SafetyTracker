package com.example.safetytracker.data.repository

import android.content.Context
import android.util.Log
import com.example.safetytracker.data.database.SafetyDatabase
import com.example.safetytracker.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class EmergencyRepository(context: Context) {
    private val database = SafetyDatabase.getDatabase(context)
    private val contactDao = database.emergencyContactDao()
    
    companion object {
        private const val TAG = "EmergencyRepository"
        
        @Volatile
        private var INSTANCE: EmergencyRepository? = null
        
        fun getInstance(context: Context): EmergencyRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EmergencyRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // Get all contacts with logging
    fun getAllContacts(): Flow<List<EmergencyContact>> {
        Log.d(TAG, "Fetching all emergency contacts")
        return contactDao.getAllContacts().map { contacts ->
            Log.d(TAG, "Retrieved ${contacts.size} emergency contacts")
            contacts
        }
    }

    // Search contacts with logging
    fun searchContacts(query: String): Flow<List<EmergencyContact>> {
        Log.d(TAG, "Searching contacts with query: '$query'")
        return if (query.isBlank()) {
            getAllContacts()
        } else {
            contactDao.searchContacts(query).map { contacts ->
                Log.d(TAG, "Search returned ${contacts.size} results for query: '$query'")
                contacts
            }
        }
    }

    // Insert contact with logging
    suspend fun insertContact(contact: EmergencyContact): Long {
        Log.d(TAG, "Inserting new contact: ${contact.name} - ${contact.phoneNumber}")
        return try {
            val id = contactDao.insertContact(contact)
            Log.d(TAG, "Successfully inserted contact with ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert contact: ${e.message}")
            throw e
        }
    }

    // Update contact with logging
    suspend fun updateContact(contact: EmergencyContact) {
        Log.d(TAG, "Updating contact: ID=${contact.id}, Name=${contact.name}")
        try {
            contactDao.updateContact(contact)
            Log.d(TAG, "Successfully updated contact ID: ${contact.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update contact ID ${contact.id}: ${e.message}")
            throw e
        }
    }

    // Delete contact with logging
    suspend fun deleteContact(contact: EmergencyContact) {
        Log.d(TAG, "Deleting contact: ID=${contact.id}, Name=${contact.name}")
        try {
            contactDao.deleteContact(contact)
            Log.d(TAG, "Successfully deleted contact ID: ${contact.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete contact ID ${contact.id}: ${e.message}")
            throw e
        }
    }

    // Delete contact by ID with logging
    suspend fun deleteContactById(contactId: Long) {
        Log.d(TAG, "Deleting contact by ID: $contactId")
        try {
            contactDao.deleteContactById(contactId)
            Log.d(TAG, "Successfully deleted contact ID: $contactId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete contact ID $contactId: ${e.message}")
            throw e
        }
    }

    // Get contact by ID with logging
    suspend fun getContactById(id: Long): EmergencyContact? {
        Log.d(TAG, "Fetching contact by ID: $id")
        return try {
            val contact = contactDao.getContactById(id)
            if (contact != null) {
                Log.d(TAG, "Found contact: ${contact.name}")
            } else {
                Log.d(TAG, "No contact found with ID: $id")
            }
            contact
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch contact ID $id: ${e.message}")
            null
        }
    }

    // Get primary contact with logging
    fun getPrimaryContact(): Flow<EmergencyContact?> {
        Log.d(TAG, "Fetching primary emergency contact")
        return contactDao.getPrimaryContact().map { contact ->
            if (contact != null) {
                Log.d(TAG, "Primary contact found: ${contact.name}")
            } else {
                Log.d(TAG, "No primary contact set")
            }
            contact
        }
    }

    // Get contact count with logging
    suspend fun getContactCount(): Int {
        Log.d(TAG, "Fetching total contact count")
        return try {
            val count = contactDao.getContactCount()
            Log.d(TAG, "Total contacts in database: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact count: ${e.message}")
            0
        }
    }
    
    // Get active contacts only with logging
    fun getActiveContacts(): Flow<List<EmergencyContact>> {
        Log.d(TAG, "Fetching active emergency contacts")
        return contactDao.getAllContacts().map { contacts ->
            val activeContacts = contacts.filter { it.isActive }
            Log.d(TAG, "Retrieved ${activeContacts.size} active contacts out of ${contacts.size} total")
            activeContacts
        }
    }
    
    // Get active contact count
    suspend fun getActiveContactCount(): Int {
        Log.d(TAG, "Fetching active contact count")

        return try {
            val allContacts = contactDao.getAllContacts().first()  // <-- colectare Flow
            val count = allContacts.count { it.isActive }
            Log.d(TAG, "Active contacts in database: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active contact count: ${e.message}")
            0
        }
    }

}
