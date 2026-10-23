package com.gal101.foodexpirytracker.ui.screens.settings // Make sure this package is correct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy // For copying the code
import androidx.compose.material.icons.filled.Delete // For kicking out members
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager // For copy functionality
import androidx.compose.ui.text.AnnotatedString // For copy functionality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButtonDefaults
import com.gal101.foodexpirytracker.firebaseSync.AuthViewModel

/**
 * Screen to display household details, members, and management options.
 *
 * @param isOwner Boolean indicating if the current user owns this household.
 * @param ownerName The name of the household owner.
 * @param joinCode The join code for this household.
 * @param membersMap Map where Key=UserId, Value=UserName for members (excluding owner).
 * @param onBack Lambda to navigate back.
 * @param onKickMember Lambda called when the owner tries to kick a member (provides UserId).
 * @param onLeaveHousehold Lambda called when a non-owner leaves the household.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseHoldScreen(
    isOwner: Boolean,
    ownerName: String, // Receive simple String
    joinCode: String,  // Receive simple String
    membersMap: Map<String, String>, // Receive the Map
    onBack: () -> Unit = {},
    onKickMember: (String) -> Unit = {}, // Receives UserId
    onLeaveHousehold: () -> Unit = {},
    onJoinHousehold: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOwner) "Your Household" else "$ownerName's Household") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- Join Code Section ---
                Text(
                    text = "Household Code",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card( // Put code in a card for better visibility
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = joinCode,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(joinCode)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // --- Members List Section ---
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start) // Align title left
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes remaining space above the leave button
                ) {
                    if (ownerName.isNotEmpty() && ownerName != "Loading...") {
                        item { // Use item for single elements
                            MemberItem(
                                username = "$ownerName (Owner)", // Identify the owner
                                showKickButton = false, // Owner cannot kick themselves
                                onKick = {}
                            )
                            Divider()
                        }
                    }

                    items(membersMap.toList()) { (memberId, memberName) -> // Correctly iterate map
                        MemberItem(
                            username = memberName,
                            showKickButton = isOwner, // Use the passed isOwner flag
                            onKick = { onKickMember(memberId) } // Pass the memberId
                        )
                        Divider()
                    }
                }

                // --- Leave Button (Conditional) ---
                if (!isOwner) {
                    Spacer(modifier = Modifier.height(16.dp)) // Space before button
                    Button(
                        onClick = onLeaveHousehold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Leave Household", fontSize = 16.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp)) // Space before button
                    Button(
                        onClick = onJoinHousehold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Join a Household", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * Helper composable for displaying a member in the list.
 */
@Composable
private fun MemberItem(
    username: String,
    showKickButton: Boolean,
    onKick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Pushes items to ends
    ) {
        Text(text = username, fontSize = 16.sp)

        if (showKickButton) {
            IconButton(onClick = onKick,
//                colors = IconButtonDefaults.iconButtonColors(
//                    containerColor = Color(0xFFED4551), // Red background
//                    contentColor = Color.White // White icon
//                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kick $username",
                    tint = Color.Gray, // Make it less prominent,
                )
            }
        }
    }
}