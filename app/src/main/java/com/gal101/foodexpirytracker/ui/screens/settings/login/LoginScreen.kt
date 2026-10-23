package com.gal101.foodexpirytracker.ui.screens.settings.login // Or your preferred package

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.* // Import common Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gal101.foodexpirytracker.R
import com.gal101.foodexpirytracker.firebaseSync.AuthOperationState
import com.gal101.foodexpirytracker.firebaseSync.AuthState // Import your AuthState enum

/**
 * Screen for user login or switching accounts. Handles loading and error states.
 *
 * @param authState The overall authentication state.
 * @param authOperationState The state of the current login/switch attempt.
 * @param currentUserEmail The email of the currently logged-in user (if permanent).
 * @param onLoginWithEmail Lambda for email/password login or switch.
 * @param onLoginWithGoogle Lambda for Google login (placeholder).
 * @param onGoToRegister Lambda to navigate to Register screen.
 * @param onBack Lambda to navigate back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
        authState: AuthState,
        authOperationState: AuthOperationState, // Receive operation state
        currentUserEmail: String?, // Receive current user's email
        onLoginWithEmail: (email: String, pass: String) -> Unit,
        onLoginWithGoogle: (idToken: String) -> Unit,
        onGoToRegister: () -> Unit,
        onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var specificError by remember { mutableStateOf<String?>(null) } // Error from ViewModel

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso =
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions
                                        .DEFAULT_SIGN_IN
                        )
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val task =
                            com.google.android.gms.auth.api.signin.GoogleSignIn
                                    .getSignedInAccountFromIntent(result.data)
                    try {
                        val account =
                                task.getResult(
                                        com.google.android.gms.common.api.ApiException::class.java
                                )
                        val idToken = account?.idToken
                        if (idToken != null) {
                            onLoginWithGoogle(idToken)
                        } else {
                            specificError = "Google Sign-In failed: Missing ID Token."
                        }
                    } catch (e: Exception) {
                        specificError = "Google Sign-In failed: ${e.message}"
                    }
                }
            }

    // --- React to state changes from ViewModel ---
    LaunchedEffect(authOperationState) {
        when (authOperationState) {
            AuthOperationState.FAILED_INVALID_CREDENTIALS ->
                    specificError = "Invalid email or password."
            AuthOperationState.FAILED_OTHER -> specificError = "An error occurred."
            AuthOperationState.SUCCESS -> {
                // Login/Switch successful, navigate back automatically
                onBack()
            }
            else -> specificError = null // Clear error for IDLE or LOADING
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Login / Switch Account") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            }
    ) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {

            // --- Display logged-in status if applicable ---
            if (authState == AuthState.LOGGED_IN && currentUserEmail != null) {
                Text(
                        text = "Currently logged in as:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                )
                Text(
                        text = currentUserEmail,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                        "Enter credentials for the account you want to switch to:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text(
                        "Login to sync your data\nacross multiple devices",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Email Field
            OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        specificError = null
                    }, // Clear error
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    isError = specificError != null // Highlight on error
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Password Field
            OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        specificError = null
                    }, // Clear error
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = specificError != null // Highlight on error
            )

            // Display error message
            if (specificError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(specificError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            } else {
                Spacer(modifier = Modifier.height(8.dp)) // Keep space consistent
            }
            Spacer(modifier = Modifier.height(16.dp)) // Space before button

            // Login / Switch Account Button
            Button(
                    onClick = {
                        specificError = null // Clear error before attempting
                        onLoginWithEmail(email, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled =
                            authOperationState !=
                                    AuthOperationState.LOADING && // Disable when loading
                            email.isNotBlank() && password.isNotBlank()
            ) {
                // Show loading indicator or text
                if (authOperationState == AuthOperationState.LOADING) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                    )
                } else {
                    val buttonText =
                            when (authState) {
                                AuthState.LOGGED_IN -> "Switch Account"
                                else -> "Login"
                            }
                    Text(buttonText)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Google Login Button
            Button(
                    onClick = {
                        specificError = null
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = authOperationState != AuthOperationState.LOADING,
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                            ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp).align(Alignment.CenterStart)
                    )

                    val googleText = buildAnnotatedString {
                        append("Sign in with ")
                        withStyle(style = SpanStyle(color = Color(0xFF4285F4))) { append("G") }
                        withStyle(style = SpanStyle(color = Color(0xFFEA4335))) { append("o") }
                        withStyle(style = SpanStyle(color = Color(0xFFFBBC05))) { append("o") }
                        withStyle(style = SpanStyle(color = Color(0xFF4285F4))) { append("g") }
                        withStyle(style = SpanStyle(color = Color(0xFF34A853))) { append("l") }
                        withStyle(style = SpanStyle(color = Color(0xFFEA4335))) { append("e") }
                    }
                    Text(text = googleText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Register Button (remains the same)
            TextButton(onClick = onGoToRegister) { Text("Don't have an account? Register here.") }
        }
    }
}
