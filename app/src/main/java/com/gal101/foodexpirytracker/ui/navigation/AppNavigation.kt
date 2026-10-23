package com.gal101.foodexpirytracker.ui.navigation

// Temporarily import empty screens (will be created in the next task)

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gal101.foodexpirytracker.firebaseSync.AuthViewModel
import com.gal101.foodexpirytracker.ui.components.ProductsViewModel
import com.gal101.foodexpirytracker.ui.screens.home.ProductList
import com.gal101.foodexpirytracker.ui.screens.manage_locations.LocationsViewModel
import com.gal101.foodexpirytracker.ui.screens.recipes.RecipesScreen
import com.gal101.foodexpirytracker.ui.screens.shoppinglist.ShoppingListScreen
import com.gal101.foodexpirytracker.ui.screens.shoppinglist.ShoppingListViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val productsViewModel: ProductsViewModel = viewModel()
    val locationsViewModel: LocationsViewModel = viewModel()
    val shoppingListViewModel: ShoppingListViewModel = viewModel()

    // Firebase authentication
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val householdId by authViewModel.currentHouseholdId.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val householdMembers by authViewModel.householdMembers.collectAsState()
    val currentUserName by authViewModel.userName.collectAsState()

    // Build a complete map including the current user.
    // Computed on every recomposition so it always reflects the latest householdMembers state.
    val allMembersMap: Map<String, String> = run {
        val uid = currentUser?.uid
        if (uid != null && !currentUserName.isNullOrBlank()) {
            householdMembers + (uid to currentUserName!!)
        } else {
            householdMembers
        }
    }

    // Keep shoppingListViewModel's userId in sync with the logged-in user
    LaunchedEffect(currentUser) { shoppingListViewModel.setCurrentUserId(currentUser?.uid ?: "") }

    LaunchedEffect(householdId) {
        if (householdId != null) {
            // I-am dat ID-ul casei, acum poate încărca produsele
            productsViewModel.getProductsListFromFirebase(householdId!!)
            locationsViewModel.startListeningToLocations(householdId!!)
            shoppingListViewModel.startListeningToShoppingList(householdId!!)
        }
        // Dacă householdId e null (ex: utilizatorul încă se creează),
        // nu facem nimic. ProductsViewModel va afișa o listă goală.
    }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    val locations by locationsViewModel.locations.collectAsState()
    val availableLocations = locations.map { it.name }

    val selectedLocations =
            remember(locations) {
                val saved = sharedPrefs.getString("selected_locations", null)
                val list = mutableStateListOf<String>()
                if (saved != null && saved.isNotEmpty()) {
                    val savedList = saved.split(",")
                    // Only keep IDs that still exist in the locations list
                    val validSavedList = savedList.filter { id -> locations.any { it.id == id } }
                    list.addAll(validSavedList)
                }

                if (list.isEmpty() && locations.isNotEmpty()) {
                    val fridgeId =
                            locations.find { it.name.equals("Fridge", ignoreCase = true) }?.id
                    if (fridgeId != null) list.add(fridgeId) else list.add(locations.first().id)
                }
                list
            }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val bottomBarScreens = listOf("my_products", "scan", "shopping_list", "recipes")
    val showBottomBar = currentRoute in bottomBarScreens

    if (currentUser == null || householdId == null) {
        if (authState == com.gal101.foodexpirytracker.firebaseSync.AuthState.LOGGED_OUT) {
            // Logged out usually means the quota is exceeded or anonymous login failed
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                ) {
                    androidx.compose.material.icons.Icons.Default.let {
                        Icon(
                                it.Warning,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                            "Autentificare Eșuată",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "Nu am putut accesa baza de date Firebase. Fie limita (Quota) gratuită a fost depășită, fie conexiunea a eșuat.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Button(
                            onClick = {
                                // Mici modificări necesită restartarea aplicației pentru a încerca
                                // iar
                                authViewModel.viewModelScope.launch {
                                    authViewModel.signInAnonymously()
                                }
                            }
                    ) { Text("Încearcă din nou") }
                }
            }
        } else {
            // Show a loading spinner while Firebase is signing in
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else {

        Scaffold(
                bottomBar = { if (showBottomBar) BottomNavBar(navController = navController) },
                topBar = {
                    if (currentRoute == "my_products") {
                        MainTopBar(
                                locations = locations,
                                selectedLocations = selectedLocations,
                                onSelectionChanged = { newSelection ->
                                    selectedLocations.clear()
                                    selectedLocations.addAll(newSelection)
                                    sharedPrefs
                                            .edit()
                                            .putString(
                                                    "selected_locations",
                                                    newSelection.joinToString(",")
                                            )
                                            .apply()
                                },
                                onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                }
        ) { innerPadding ->
            NavHost(
                    navController = navController,
                    startDestination = "my_products",
                    modifier = Modifier.padding(innerPadding)
            ) {
                composable("my_products") {
                    ProductList(
                            viewModel = productsViewModel,
                            shoppingListViewModel = shoppingListViewModel,
                            selectedLocations = selectedLocations,
                            locations = locations
                    )
                }

                composable("scan") {
                    // Directly open the AddProductNav starting at scan
                    AddProductNav(navController, productsViewModel, locationsViewModel)
                }

                composable("shopping_list") {
                    ShoppingListScreen(
                            viewModel = shoppingListViewModel,
                            membersMap = allMembersMap
                    )
                }

                composable("recipes") { RecipesScreen() }

                composable("settings") {
                    SettingsNavigation(navController, authViewModel, locationsViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
        locations: List<com.gal101.foodexpirytracker.data.model.CustomLocation>,
        selectedLocations: List<String>,
        onSelectionChanged: (List<String>) -> Unit,
        onSettingsClick: () -> Unit
) {
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    CenterAlignedTopAppBar(
            title = {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier =
                                    Modifier.clickable { expanded = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val titleText =
                                when {
                                    locations.isEmpty() -> "No Locations"
                                    selectedLocations.isEmpty() -> "No Location"
                                    selectedLocations.size == locations.size -> "All Locations"
                                    selectedLocations.size == 1 -> {
                                        locations.find { it.id == selectedLocations.first() }?.name
                                                ?: "Unknown"
                                    }
                                    else -> "${selectedLocations.size} Locations"
                                }

                        val titleIcon =
                                when {
                                    selectedLocations.isEmpty() || locations.isEmpty() ->
                                            androidx.compose.material.icons.Icons.Default.Close
                                    selectedLocations.size == 1 -> {
                                        val loc =
                                                locations.find {
                                                    it.id == selectedLocations.first()
                                                }
                                        if (loc != null)
                                                com.gal101.foodexpirytracker.utils
                                                        .LocationIconMapper.getIconFromName(
                                                        loc.iconName
                                                )
                                        else androidx.compose.material.icons.Icons.Default.Close
                                    }
                                    else -> null
                                }

                        if (titleIcon != null) {
                            Icon(
                                    imageVector = titleIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                                text = titleText,
                                style =
                                        MaterialTheme.typography.titleLarge.copy(
                                                fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight
                                                                .Bold
                                        ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (locations.isNotEmpty()) {
                            val allSelected = selectedLocations.size == locations.size
                            DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = allSelected, onCheckedChange = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                    "All Locations",
                                                    fontWeight =
                                                            androidx.compose.ui.text.font.FontWeight
                                                                    .Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (allSelected) {
                                            onSelectionChanged(emptyList())
                                        } else {
                                            onSelectionChanged(locations.map { it.id })
                                        }
                                    }
                            )
                            Divider()
                        }

                        locations.forEach { loc ->
                            val icon =
                                    com.gal101.foodexpirytracker.utils.LocationIconMapper
                                            .getIconFromName(loc.iconName)
                            DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                    checked = selectedLocations.contains(loc.id),
                                                    onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                    imageVector = icon,
                                                    contentDescription = loc.name,
                                                    modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(loc.name)
                                        }
                                    },
                                    onClick = {
                                        val mutableList = selectedLocations.toMutableList()
                                        if (mutableList.contains(loc.id)) mutableList.remove(loc.id)
                                        else mutableList.add(loc.id)
                                        onSelectionChanged(mutableList)
                                    }
                            )
                        }
                        if (locations.isEmpty()) {
                            DropdownMenuItem(
                                    text = { Text("No custom locations yet") },
                                    onClick = { expanded = false }
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                // Placeholder to perfectly balance the Settings icon on the other side
                Spacer(modifier = Modifier.size(48.dp))
            },
            modifier = Modifier.fillMaxWidth()
    )
}
