package com.gal101.foodexpirytracker.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme // Import for error color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gal101.foodexpirytracker.firebaseSync.JoinState

/**
 * Screen for entering a household join code.
 *
 * @param onJoin Lambda function called when the "Join" button is pressed.
 * It receives the entered code as a String.
 * Returns true on success, false on failure.
 * @param onBack Lambda function to navigate back.
 * @param personalHouseCode The user's own household code (to prevent joining self).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseHoldJoinScreen(
    joinState: JoinState,
    onJoin: (String) -> Unit,
    onBack: () -> Unit = {},
    personalHouseCode: String
) {
    var code by remember { mutableStateOf("") }
    // --- NEW: State for the error message ---
    val errorMessage = if (joinState == JoinState.ERROR) "Invalid or incorrect code." else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join a Household") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter the join code shared with you:", fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.uppercase()
                },
                label = { Text("Join Code (e.g., ABC-123)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                // --- NEW: Highlight field on error ---
                isError = errorMessage != null
            )

            // Display error message based on state
            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            } else {
                Spacer(Modifier.height(22.dp))
            }

            Spacer(Modifier.height(16.dp)) // Spacing before button

            Button(
                onClick = {
                    // --- MODIFIED: Set error message on failure ---
                    if (joinState != JoinState.LOADING) { // Prevent double clicks
                        onJoin(code) // Call the lambda
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = checkCodeFormat(code, personalHouseCode) && joinState != JoinState.LOADING
            ) {
                // Show spinner inside button when loading
                if (joinState == JoinState.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Join Household", fontSize = 16.sp)
                }
            }
        }
    }
}

// checkCodeFormat function remains the same
fun checkCodeFormat(code: String, personalCode: String): Boolean {
    // ... (your existing checks)
    if(code.length != 7) return false;
    if(code[3] != '-') return false;
    if(code == personalCode) return false; // Maybe add a specific error message for this later
    return true;
}