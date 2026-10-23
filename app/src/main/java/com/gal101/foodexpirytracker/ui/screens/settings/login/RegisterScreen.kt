package com.gal101.foodexpirytracker.ui.screens.settings.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gal101.foodexpirytracker.firebaseSync.AuthOperationState // Import the state enum

/**
 * Screen for registering a new permanent account.
 * Handles loading and error states. Navigates back on success.
 *
 * @param authOperationState The current state of the registration attempt.
 * @param onRegister Lambda called with username, email, and password when Register is clicked.
 * @param onBack Lambda to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authOperationState: AuthOperationState,
    onRegister: (username: String, email: String, pass: String) -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMismatchError by remember { mutableStateOf<String?>(null) } // Local password mismatch error
    var specificError by remember { mutableStateOf<String?>(null) } // Error from ViewModel

    // --- React to state changes from ViewModel ---
    LaunchedEffect(authOperationState) {
        when (authOperationState) {
            AuthOperationState.FAILED_EMAIL_EXISTS -> specificError = "This email is already registered."
            AuthOperationState.FAILED_WEAK_PASSWORD -> specificError = "Password is too weak (min 6 chars)."
            AuthOperationState.FAILED_OTHER -> specificError = "An unknown error occurred. Please try again."
            AuthOperationState.SUCCESS -> {
                // Registration successful, navigate back automatically
                onBack()
            }
            else -> specificError = null // Clear specific error for IDLE or LOADING
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Creating an account makes it easier to sync your data across multiple devices",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; specificError = null }, // Clear error on change
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = username.contains("/") || specificError != null // Show error if specific error exists
            )
            // ... (Username validation text)

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; specificError = null }, // Clear error on change
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = specificError != null // Show error if specific error exists
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordMismatchError = null; specificError = null }, // Clear errors
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordMismatchError != null || specificError != null // Show error
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; passwordMismatchError = null; specificError = null }, // Clear errors
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordMismatchError != null // Only show mismatch error here
            )

            // Show combined error message (prefer specific error over mismatch)
            val displayError = specificError ?: passwordMismatchError
            if (displayError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(displayError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            } else {
                Spacer(modifier = Modifier.height(8.dp)) // Keep space consistent
            }
            Spacer(modifier = Modifier.height(16.dp)) // Space before button

            // Register Button
            Button(
                onClick = {
                    passwordMismatchError = null // Clear local error first
                    specificError = null       // Clear specific error
                    if (password == confirmPassword) {
                        onRegister(username.trim(), email.trim(), password)
                    } else {
                        passwordMismatchError = "Passwords do not match."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = authOperationState != AuthOperationState.LOADING && // Disable button when loading
                        username.isNotBlank() && !username.contains("/") &&
                        email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() &&
                        password.length >= 6
            ) {
                // Show loading indicator or text
                if (authOperationState == AuthOperationState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register")
                }
            }
            // ... (Password length text)
            // ... (Divider and "Already have an account?" button using onBack)
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { // Use onBack to go to Login screen
                Text("Already have an account? Login here.")
            }
        }
    }
}