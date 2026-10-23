package com.gal101.foodexpirytracker.ui.screens.manage_locations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gal101.foodexpirytracker.data.model.CustomLocation
import com.gal101.foodexpirytracker.utils.LocationIconMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLocationsScreen(viewModel: LocationsViewModel, onBack: () -> Unit) {
    val locations by viewModel.locations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<CustomLocation?>(null) }
    var locationToDelete by remember { mutableStateOf<CustomLocation?>(null) }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text("Manage Locations") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Location")
                }
            }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (locations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                            "No custom locations found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations, key = { it.id }) { location ->
                        LocationItemRow(
                                location = location,
                                onEdit = { editingLocation = location },
                                onDelete = { locationToDelete = location }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            if (locations.size >= 9) {
                val context = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    android.widget.Toast.makeText(
                                    context,
                                    "Maximum 9 locations allowed.",
                                    android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                    showAddDialog = false
                }
            } else {
                LocationDialog(
                        initialName = "",
                        initialIcon = "",
                        onDismiss = { showAddDialog = false },
                        onConfirm = { name, iconName ->
                            viewModel.addLocation(name, iconName)
                            showAddDialog = false
                        }
                )
            }
        }

        editingLocation?.let { loc ->
            LocationDialog(
                    initialName = loc.name,
                    initialIcon = loc.iconName,
                    onDismiss = { editingLocation = null },
                    onConfirm = { newName, newIcon ->
                        viewModel.updateLocation(loc.id, newName, newIcon)
                        editingLocation = null
                    }
            )
        }

        locationToDelete?.let { loc ->
            AlertDialog(
                    onDismissRequest = { locationToDelete = null },
                    title = { Text("Delete Location?") },
                    text = {
                        Text(
                                "Are you sure you want to delete '${loc.name}'? Deleting this location will also delete all products inside it."
                        )
                    },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    viewModel.deleteLocation(loc.id)
                                    locationToDelete = null
                                }
                        ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { locationToDelete = null }) { Text("Cancel") }
                    }
            )
        }
    }
}

@Composable
fun LocationItemRow(location: CustomLocation, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
            shape = RoundedCornerShape(12.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = LocationIconMapper.getIconFromName(location.iconName)
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(12.dp)
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = icon,
                        contentDescription = location.name,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) {
                Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LocationDialog(
        initialName: String,
        initialIcon: String,
        onDismiss: () -> Unit,
        onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember {
        mutableStateOf(if (initialIcon.isEmpty()) "kitchen" else initialIcon)
    }

    AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (initialName.isEmpty()) "Add Location" else "Edit Location") },
            text = {
                Column {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Location Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                            "Select Icon",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(140.dp)
                    ) {
                        items(LocationIconMapper.predefinedIcons) { (iconName, vector) ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                    modifier =
                                            Modifier.size(48.dp)
                                                    .background(
                                                            if (isSelected)
                                                                    MaterialTheme.colorScheme
                                                                            .primaryContainer
                                                            else Color.Transparent,
                                                            RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedIcon = iconName },
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                        imageVector = vector,
                                        contentDescription = iconName,
                                        tint =
                                                if (isSelected)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onConfirm(name, selectedIcon) },
                        enabled = name.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
