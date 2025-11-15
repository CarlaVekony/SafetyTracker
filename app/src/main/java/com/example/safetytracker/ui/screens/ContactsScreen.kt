package com.example.safetytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.safetytracker.data.database.SafetyDatabase
import com.example.safetytracker.data.model.EmergencyContact
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: State<List<EmergencyContact>> = remember { mutableStateOf(emptyList()) },
    onAddContact: (name: String, phone: String) -> Unit = { _, _ -> }
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Input fields
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                onAddContact(name, phoneNumber)
                name = ""
                phoneNumber = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Contact")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contact list
        LazyColumn {
            items(contacts.value) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = contact.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                        if (contact.isPrimary) {
                            Text("Primary Contact", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsScreenWithData() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { SafetyDatabase.getDatabase(context) }
    val contacts = database.emergencyContactDao().getAllContacts().collectAsState(initial = emptyList())
    
    ContactsScreen(
        contacts = contacts,
        onAddContact = { name, phone ->
            scope.launch {
                val contact = EmergencyContact(
                    name = name,
                    phoneNumber = phone
                )
                database.emergencyContactDao().insertContact(contact)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenPreview() {
    SafetyTrackerTheme {
        val previewContacts = remember {
            mutableStateOf(
                listOf(
                    EmergencyContact(1, "John Doe", "+1234567890", true),
                    EmergencyContact(2, "Jane Smith", "+0987654321", false),
                    EmergencyContact(3, "Emergency Services", "112", false)
                )
            )
        }
        
        ContactsScreen(
            contacts = previewContacts,
            onAddContact = { _, _ -> }
        )
    }
}
