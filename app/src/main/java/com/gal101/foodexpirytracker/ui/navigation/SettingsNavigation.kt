package com.gal101.foodexpirytracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gal101.foodexpirytracker.firebaseSync.AuthOperationState
import com.gal101.foodexpirytracker.firebaseSync.AuthViewModel
import com.gal101.foodexpirytracker.firebaseSync.JoinState
import com.gal101.foodexpirytracker.ui.screens.manage_locations.LocationsViewModel
import com.gal101.foodexpirytracker.ui.screens.manage_locations.ManageLocationsScreen
import com.gal101.foodexpirytracker.ui.screens.settings.HouseHoldJoinScreen
import com.gal101.foodexpirytracker.ui.screens.settings.HouseHoldScreen
import com.gal101.foodexpirytracker.ui.screens.settings.SettingsScreen
import com.gal101.foodexpirytracker.ui.screens.settings.login.LoginScreen
import com.gal101.foodexpirytracker.ui.screens.settings.login.RegisterScreen
import kotlinx.coroutines.launch

@Composable
fun SettingsNavigation(
        parentNavController: NavController,
        authViewModel: AuthViewModel,
        locationsViewModel: LocationsViewModel
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val joinCode by authViewModel.joinCode.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val isOwner by authViewModel.isViewingPersonalHousehold.collectAsState()
    // val ownerId by authViewModel.householdOwnerId.collectAsState()
    val ownerName by authViewModel.householdOwnerName.collectAsState()
    val ownerId by authViewModel.householdOwnerId.collectAsState()
    val membersMap by authViewModel.householdMembers.collectAsState()
    // Filter out ownerId so the owner doesn't appear twice in HouseholdScreen
    // (owner is already shown separately as "Name (Owner)")
    val membersMapWithoutOwner = if (ownerId != null) membersMap - ownerId!! else membersMap
    val authOperationState: AuthOperationState by authViewModel.authOperationState.collectAsState()
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()
    // val currentUser by authViewModel.currentUser.collectAsState()
    NavHost(navController = navController, startDestination = "settings") {
        composable("settings") {
            SettingsScreen(
                    onGoToLogin = { navController.navigate("login_screen") },
                    onBack = { parentNavController.popBackStack() },
                    onYourHousehold = { navController.navigate("household") },
                    onManageLocations = { navController.navigate("manage_locations") },
                    authState = authState,
                    // householdCode = joinCode ?: "Loading...",
                    userName = userName ?: "No user",
                    onNameChange = { newName ->
                        scope.launch { authViewModel.changeUsername(newName) }
                    }
            )
        }

        composable("manage_locations") {
            ManageLocationsScreen(
                    viewModel = locationsViewModel,
                    onBack = { navController.popBackStack() }
            )
        }

        composable("login_screen") {
            LaunchedEffect(Unit) { authViewModel.resetAuthOperationState() }

            LoginScreen(
                    // onNext = {},
                    onBack = { navController.popBackStack() },
                    authState = authState,
                    onGoToRegister = { navController.navigate("register") },
                    onLoginWithEmail = { email, pass ->
                        scope.launch { authViewModel.signInWithEmail(email, pass) }
                    },
                    onLoginWithGoogle = { idToken ->
                        scope.launch { authViewModel.signInWithGoogle(idToken) }
                    },
                    authOperationState = authOperationState,
                    currentUserEmail = currentUserEmail
            )
        }

        composable("register") {
            LaunchedEffect(Unit) { authViewModel.resetAuthOperationState() }

            RegisterScreen(
                    authOperationState = authOperationState,
                    onRegister = { username, email, pass ->
                        authViewModel.register(username, email, pass)
                    },
                    onBack = { navController.popBackStack() },
                    // onGoToLogin = {}
                    )
        }

        composable("household") {
            LaunchedEffect(Unit) { // 'Unit' means run once when the screen appears
                // Manually get the current IDs and trigger fetch
                val currentIds = authViewModel.householdMemberIds.value
                authViewModel.fetchMemberNames(
                        currentIds
                ) // Assuming you make fetchMemberNames public
            }

            HouseHoldScreen(
                    isOwner = isOwner,
                    // Pass the actual owner name, provide a default if null
                    ownerName = ownerName ?: "Loading...",
                    // Pass the join code, provide a default if null
                    joinCode = joinCode ?: "Loading...",
                    // Pass the map of members
                    membersMap = membersMapWithoutOwner,
                    onBack = { navController.popBackStack() },
                    onKickMember = { userIdToKick ->
                        // Launch the suspend function in a coroutine
                        scope.launch { authViewModel.kickMember(userIdToKick) }
                    },
                    onLeaveHousehold = {
                        // Launch the suspend function in a coroutine
                        scope.launch {
                            authViewModel.leaveHousehold()
                            // Optionally navigate back after leaving
                            // navController.popBackStack()
                        }
                    },
                    onJoinHousehold = { navController.navigate("household_join") }
            )
        }

        composable("household_join") {
            val joinState by authViewModel.joinState.collectAsState()

            LaunchedEffect(joinState) {
                if (joinState == JoinState.SUCCESS) {
                    navController.popBackStack() // Navigate back on success
                    authViewModel.resetJoinState() // Reset state after navigation
                }
            }

            DisposableEffect(Unit) { onDispose { authViewModel.resetJoinState() } }

            HouseHoldJoinScreen(
                    onJoin = { code ->
                        // Just launch the suspend function, don't worry about return value here
                        scope.launch {
                            authViewModel.joinHousehold(code)
                            // The LaunchedEffect above will handle navigation on SUCCESS
                            // The UI will show error based on ERROR state
                        }
                    },
                    onBack = {
                        authViewModel.resetJoinState()
                        navController.popBackStack()
                    },
                    personalHouseCode = joinCode ?: "",
                    joinState = joinState
            )
        }
    }
}
