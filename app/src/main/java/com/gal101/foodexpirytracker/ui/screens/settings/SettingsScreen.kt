package com.gal101.foodexpirytracker.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Import everything from layout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit // <-- Import Edit icon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.* // Import everything from material3
import androidx.compose.runtime.* // Import everything from runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gal101.foodexpirytracker.firebaseSync.AuthState // Correct import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
        authState: AuthState,
        userName: String = "User",
        // householdCode: String,
        onGoToLogin: () -> Unit,
        // onLogout: () -> Unit,
        onNameChange: (String) -> Unit, // Function to call when name changes
        onYourHousehold: () -> Unit,
        onManageLocations: () -> Unit,
        onBack: () -> Unit
) {
    // --- NEW: State to control dialog visibility ---
    var showEditUsernameDialog by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                        }
                )
            }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {

                // --- 1. Profile Section ---
                Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    // --- MODIFIED: Row for username and edit button ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = userName, style = MaterialTheme.typography.titleMedium)
                        // --- NEW: Edit Button ---
                        IconButton(
                                onClick = { showEditUsernameDialog = true }, // Open dialog
                                modifier = Modifier.size(24.dp).padding(start = 10.dp)
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Username",
                                    tint = MaterialTheme.colorScheme.primary // Use theme color
                            )
                        }
                    }
                    // --- End Modification ---
                }

                Divider()

                // --- 2. Settings List (remains the same) ---
                Column(modifier = Modifier.padding(16.dp)) {
                    // ... (Your existing SettingsItems for Login/Logout, Your Household) ...
                    when (authState) {
                        AuthState.LOGGED_IN_ANONYMOUSLY -> {
                            SettingsItem(
                                    text = "Register / Sign In",
                                    icon = Icons.Default.Login,
                                    onClick = onGoToLogin
                            )
                        }
                        AuthState.LOGGED_IN -> {
                            SettingsItem(
                                    text = "Switch Account", // Changed from Logout based on our
                                    // discussion
                                    icon =
                                            Icons.Default
                                                    .Login, // Maybe use Icons.Default.SwitchAccount
                                    // ?
                                    onClick = onGoToLogin // Reuse login flow for switching
                            )
                        }
                        else -> {}
                    }
                    Spacer(Modifier.height(8.dp))
                    SettingsItem(
                            text = "Your Household",
                            icon = Icons.Default.Home,
                            onClick = onYourHousehold
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsItem(
                            text = "Manage Locations",
                            icon = Icons.Default.LocationOn,
                            onClick = onManageLocations
                    )
                    // ... (Your commented-out code)
                }
            }

            Text(
                    text = "FoodExpiryTracker by gal101",
                    style = MaterialTheme.typography.bodySmall, // Use a small text style
                    color = MaterialTheme.colorScheme.primary, // Use a subtle color
                    modifier =
                            Modifier.align(
                                            Alignment.BottomCenter
                                    ) // Align to bottom center of the Box
                                    .padding(bottom = 16.dp) // Add some padding from the edge
            )
        }

        // --- NEW: Conditionally display the dialog ---
        if (showEditUsernameDialog) {
            EditUsernameDialog(
                    currentUsername = userName, // Pass current username
                    onDismiss = { showEditUsernameDialog = false }, // Close dialog
                    onConfirm = { newName ->
                        onNameChange(newName) // Call the lambda passed to SettingsScreen
                        showEditUsernameDialog = false // Close dialog
                    }
            )
        }
    }
}

// --- Your existing helper Composables (SettingsItem, SettingsInfoText) ---
// ... (Keep them as they are) ...
/**
 *
 * A helper composable to create a consistent, clickable list item.
 */
@Composable
private fun SettingsItem(text: String, icon: ImageVector, onClick: () -> Unit) {

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = text, fontSize = 16.sp)
    }
}

/**
 *
 * A helper composable for displaying non-clickable info.
 */
@Composable
private fun SettingsInfoText(label: String, value: String) {

    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.width(8.dp))

        // Here you might want to add a "click to copy" interaction

        Text(text = value, fontSize = 16.sp)
    }
}

// --- NEW: Adapted Dialog Composable ---
/**
 * Dialog for editing the username.
 * @param currentUsername The current username to pre-fill the field (optional).
 * @param onDismiss Lambda called when the dialog is dismissed.
 * @param onConfirm Lambda called with the new username when confirmed.
 */
@Composable
fun EditUsernameDialog(
        currentUsername: String = "",
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
) {
    // State to hold the text in the dialog's TextField
    var text by remember { mutableStateOf(currentUsername) }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Change Username") }, // Changed title
            text = {
                Column {
                    Text("Enter your new username:") // Changed prompt
                    Spacer(Modifier.height(8.dp))
                    TextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            // placeholder = { Text("E.g., My Device") } // Changed placeholder
                            )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            onConfirm(text.trim()) // Trim whitespace before confirming
                            onDismiss()
                        },
                        // Keep similar validation: not blank, no slashes
                        enabled = text.isNotBlank() && !text.contains('/')
                ) {
                    Text("Save") // Changed button text
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel") // Changed button text
                }
            }
    )
}
