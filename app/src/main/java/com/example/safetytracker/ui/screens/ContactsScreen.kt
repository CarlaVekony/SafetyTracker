package com.example.safetytracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.safetytracker.data.model.EmergencyContact
import com.example.safetytracker.data.repository.EmergencyRepository
import com.example.safetytracker.ui.theme.SafetyTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ContactsScreen(
    contacts: State<List<EmergencyContact>> = remember { mutableStateOf(emptyList()) },
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddContact: (name: String, phone: String) -> Unit = { _, _ -> },
    onDeleteContact: (EmergencyContact) -> Unit = {},
    onEditContact: (EmergencyContact) -> Unit = {},
    onUpdateContactActive: (EmergencyContact, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<EmergencyContact?>(null) }
    var isAddingContact by remember { mutableStateOf(false) }
    var deletingContactId by remember { mutableStateOf<Long?>(null) }
    var showManageActiveDialog by remember { mutableStateOf(false) }
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300)
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Emergency Contacts",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Search Bar with animation
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, delayMillis = 100)
                )
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search contacts") },
                    placeholder = { Text("Search by name or phone number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Add Contact Section with animation
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, delayMillis = 200)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Add New Contact",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isAddingContact
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isAddingContact
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                                    isAddingContact = true
                                    onAddContact(name.trim(), phoneNumber.trim())
                                    name = ""
                                    phoneNumber = ""
                                    // Reset loading state after delay
                                    kotlinx.coroutines.GlobalScope.launch {
                                        delay(1000)
                                        isAddingContact = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = name.isNotBlank() && phoneNumber.isNotBlank() && !isAddingContact
                        ) {
                            if (isAddingContact) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Adding...")
                                }
                            } else {
                                Text("Add Contact")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Manage Active Contacts Section
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300, delayMillis = 300)
                )
            ) {
                OutlinedButton(
                    onClick = { showManageActiveDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manage",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Active Contacts")
                    Spacer(modifier = Modifier.width(8.dp))
                    val activeCount = contacts.value.count { it.isActive }
                    Text(
                        text = "($activeCount/${contacts.value.size} active)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contacts List Section
            Text(
                text = "Saved Contacts (${contacts.value.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (contacts.value.isEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No contacts found for \"$searchQuery\""
                            } else {
                                "No emergency contacts added yet.\nAdd your first contact above."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts.value, key = { it.id }) { contact ->
                        SwipeableContactCard(
                            contact = contact,
                            isDeleting = deletingContactId == contact.id,
                            onDeleteClick = { 
                                deletingContactId = contact.id
                                showDeleteDialog = contact 
                            },
                            onEditClick = { onEditContact(contact) },
                            onSwipeDelete = { 
                                deletingContactId = contact.id
                                showDeleteDialog = contact 
                            },
                            onSwipeEdit = { onEditContact(contact) }
                        )
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog with animation
    showDeleteDialog?.let { contactToDelete ->
        var dialogVisible by remember { mutableStateOf(false) }
        
        LaunchedEffect(contactToDelete) {
            dialogVisible = true
        }
        
        AnimatedVisibility(
            visible = dialogVisible,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(200)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            AlertDialog(
                onDismissRequest = { 
                    dialogVisible = false
                    showDeleteDialog = null
                    deletingContactId = null
                },
                title = { Text("Delete Contact") },
                text = {
                    Text("Are you sure you want to delete \"${contactToDelete.name}\"?\n\nThis action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteContact(contactToDelete)
                            dialogVisible = false
                            showDeleteDialog = null
                            // Reset deleting state after delay
                            kotlinx.coroutines.GlobalScope.launch {
                                delay(500)
                                deletingContactId = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (deletingContactId == contactToDelete.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Deleting...")
                            }
                        } else {
                            Text("Delete")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            dialogVisible = false
                            showDeleteDialog = null
                            deletingContactId = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    
    // Manage Active Contacts Dialog
    if (showManageActiveDialog) {
        var tempActiveStates by remember { 
            mutableStateOf(contacts.value.associate { it.id to it.isActive }) 
        }
        
        AlertDialog(
            onDismissRequest = { showManageActiveDialog = false },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Active Contacts")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Select which contacts should receive emergency alerts:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(contacts.value, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = LocalIndication.current
                                    ) {
                                        tempActiveStates = tempActiveStates.toMutableMap().apply {
                                            this[contact.id] = !(this[contact.id] ?: false)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tempActiveStates[contact.id] ?: false,
                                    onCheckedChange = { isChecked ->
                                        tempActiveStates = tempActiveStates.toMutableMap().apply {
                                            this[contact.id] = isChecked
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Status indicator
                                val isActive = tempActiveStates[contact.id] ?: false
                                Icon(
                                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (isActive) "Active" else "Inactive",
                                    tint = if (isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    val selectedCount = tempActiveStates.values.count { it }
                    Text(
                        text = "$selectedCount of ${contacts.value.size} contacts selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Update contacts with new active states
                        contacts.value.forEach { contact ->
                            val newActiveState = tempActiveStates[contact.id] ?: contact.isActive
                            if (newActiveState != contact.isActive) {
                                onUpdateContactActive(contact, newActiveState)
                            }
                        }
                        showManageActiveDialog = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManageActiveDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactCard(
    contact: EmergencyContact,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (contact.isActive) "Active contact" else "Inactive contact",
                        tint = if (contact.isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(8.dp)
                    )
                }
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Primary Contact",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(
                onClick = onDeleteClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${contact.name}"
                )
            }
        }
    }
}

@Composable
fun ContactsScreenWithData() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { EmergencyRepository.getInstance(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf<EmergencyContact?>(null) }
    
    val contacts = repository.searchContacts(searchQuery).collectAsState(initial = emptyList())
    
    ContactsScreen(
        contacts = contacts,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onAddContact = { name, phone ->
            scope.launch {
                repository.insertContact(
                    EmergencyContact(
                        name = name,
                        phoneNumber = phone
                    )
                )
            }
        },
        onDeleteContact = { contact ->
            scope.launch {
                repository.deleteContact(contact)
            }
        },
        onEditContact = { contact ->
            showEditDialog = contact
        },
        onUpdateContactActive = { contact, isActive ->
            scope.launch {
                repository.updateContact(
                    contact.copy(isActive = isActive)
                )
            }
        }
    )
    
    // Edit Contact Dialog
    showEditDialog?.let { contactToEdit ->
        EditContactDialog(
            contact = contactToEdit,
            onDismiss = { showEditDialog = null },
            onConfirm = { updatedName, updatedPhone ->
                scope.launch {
                    repository.updateContact(
                        contactToEdit.copy(
                            name = updatedName.trim(),
                            phoneNumber = updatedPhone.trim()
                        )
                    )
                    showEditDialog = null
                }
            }
        )
    }
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
                    EmergencyContact(3, "Emergency Services", "112", false),
                    EmergencyContact(4, "Dr. Wilson", "+1555123456", false)
                )
            )
        }
        
        ContactsScreen(
            contacts = previewContacts,
            searchQuery = "",
            onSearchQueryChange = {},
            onAddContact = { _, _ -> },
            onDeleteContact = {},
            onEditContact = {}
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeableContactCard(
    contact: EmergencyContact,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onSwipeDelete: () -> Unit,
    onSwipeEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberDismissState(
        confirmStateChange = { dismissValue ->
            when (dismissValue) {
                DismissValue.DismissedToStart -> {
                    onSwipeDelete()
                    false // Don't actually dismiss, show dialog instead
                }
                DismissValue.DismissedToEnd -> {
                    onSwipeEdit()
                    false // Don't actually dismiss, handle edit
                }
                else -> false
            }
        }
    )
    
    SwipeToDismiss(
        state = dismissState,
        modifier = modifier,
        background = {
            SwipeBackground(
                dismissState = dismissState,
                onDeleteClick = onDeleteClick,
                onEditClick = onEditClick
            )
        },
        dismissContent = {
            AnimatedContactCard(
                contact = contact,
                isDeleting = isDeleting,
                onDeleteClick = onDeleteClick
            )
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeBackground(
    dismissState: DismissState,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val direction = dismissState.dismissDirection ?: return
    val color by animateColorAsState(
        when (direction) {
            DismissDirection.StartToEnd -> MaterialTheme.colorScheme.primary
            DismissDirection.EndToStart -> MaterialTheme.colorScheme.error
        }, label = "SwipeBackgroundColor"
    )
    
    val alignment = when (direction) {
        DismissDirection.StartToEnd -> Alignment.CenterStart
        DismissDirection.EndToStart -> Alignment.CenterEnd
    }
    
    val icon = when (direction) {
        DismissDirection.StartToEnd -> Icons.Default.Edit
        DismissDirection.EndToStart -> Icons.Default.Delete
    }
    
    val scale by animateFloatAsState(
        if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f,
        label = "SwipeIconScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (direction) {
                DismissDirection.StartToEnd -> "Edit"
                DismissDirection.EndToStart -> "Delete"
            },
            modifier = Modifier.scale(scale),
            tint = Color.White
        )
    }
}

@Composable
private fun AnimatedContactCard(
    contact: EmergencyContact,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(contact.id) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = if (contact.isActive) "Active contact" else "Inactive contact",
                            tint = if (contact.isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(8.dp)
                        )
                    }
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (contact.isPrimary) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Primary Contact",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    val scale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ), label = "DeleteButtonScale"
                    )
                    
                    IconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.scale(scale)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${contact.name}"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditContactDialog(
    contact: EmergencyContact,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var editedName by remember { mutableStateOf(contact.name) }
    var editedPhone by remember { mutableStateOf(contact.phoneNumber) }
    var isUpdating by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(contact) {
        dialogVisible = true
    }
    
    AnimatedVisibility(
        visible = dialogVisible,
        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(200)
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200)
        )
    ) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) {
                    dialogVisible = false
                    onDismiss()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Contact")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    OutlinedTextField(
                        value = editedPhone,
                        onValueChange = { editedPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isUpdating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedName.isNotBlank() && editedPhone.isNotBlank()) {
                            isUpdating = true
                            onConfirm(editedName, editedPhone)
                            // Reset updating state after delay
                            kotlinx.coroutines.GlobalScope.launch {
                                delay(500)
                                isUpdating = false
                            }
                        }
                    },
                    enabled = editedName.isNotBlank() && editedPhone.isNotBlank() && !isUpdating
                ) {
                    if (isUpdating) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating...")
                        }
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isUpdating) {
                            dialogVisible = false
                            onDismiss()
                        }
                    },
                    enabled = !isUpdating
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
