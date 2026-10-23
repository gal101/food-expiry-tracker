package com.gal101.foodexpirytracker.ui.screens.shoppinglist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gal101.foodexpirytracker.data.model.ShoppingListItem
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
        viewModel: ShoppingListViewModel,
        membersMap: Map<String, String> = emptyMap()
) {
        val allItems by viewModel.items.collectAsState()
        var showAddDialog by remember { mutableStateOf(false) }
        var editingItem by remember { mutableStateOf<ShoppingListItem?>(null) }

        val checkedItems = allItems.filter { it.isChecked }
        val uncheckedItems = allItems.filter { !it.isChecked }
        val displayItems = uncheckedItems + checkedItems // unchecked first, checked at bottom

        val checkedCount = checkedItems.size

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                        "Shopping List",
                                                        style = MaterialTheme.typography.titleLarge
                                                )
                                                if (allItems.isNotEmpty()) {
                                                        Text(
                                                                "${allItems.size} items",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                },
                                actions = {
                                        if (checkedCount > 0) {
                                                IconButton(
                                                        onClick = { viewModel.deleteCheckedItems() }
                                                ) {
                                                        Icon(
                                                                Icons.Default.DeleteSweep,
                                                                contentDescription =
                                                                        "Clear Checked",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .error
                                                        )
                                                }
                                        }
                                }
                        )
                },
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = { showAddDialog = true },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                        ) { Icon(Icons.Default.Add, contentDescription = "Add item") }
                }
        ) { paddingValues ->
                if (allItems.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🛒", style = MaterialTheme.typography.displayMedium)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                                "Your shopping list is empty",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                "Tap + to add items",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.8f)
                                        )
                                }
                        }
                } else {
                        LazyColumn(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
                        ) {
                                // Unchecked items section
                                if (uncheckedItems.isNotEmpty()) {
                                        items(uncheckedItems, key = { it.id }) { item ->
                                                SwipeToDeleteShoppingItem(
                                                        item = item,
                                                        membersMap = membersMap,
                                                        onToggle = { newChecked ->
                                                                viewModel.toggleItemChecked(
                                                                        item.id,
                                                                        newChecked
                                                                )
                                                        },
                                                        onDelete = {
                                                                viewModel.deleteItem(item.id)
                                                        },
                                                        onEdit = { editingItem = item }
                                                )
                                        }
                                }

                                // Divider before checked items
                                if (checkedItems.isNotEmpty() && uncheckedItems.isNotEmpty()) {
                                        item {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        HorizontalDivider(
                                                                modifier = Modifier.weight(1f),
                                                                thickness = 0.5.dp
                                                        )
                                                        Text(
                                                                "  ${checkedItems.size} checked  ",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                                                .copy(alpha = 0.6f)
                                                        )
                                                        HorizontalDivider(
                                                                modifier = Modifier.weight(1f),
                                                                thickness = 0.5.dp
                                                        )
                                                }
                                        }
                                }

                                // Checked items section
                                if (checkedItems.isNotEmpty()) {
                                        items(checkedItems, key = { it.id }) { item ->
                                                SwipeToDeleteShoppingItem(
                                                        item = item,
                                                        membersMap = membersMap,
                                                        onToggle = { newChecked ->
                                                                viewModel.toggleItemChecked(
                                                                        item.id,
                                                                        newChecked
                                                                )
                                                        },
                                                        onDelete = {
                                                                viewModel.deleteItem(item.id)
                                                        },
                                                        onEdit = { editingItem = item }
                                                )
                                        }
                                }
                        }
                }
        }

        // Add dialog
        if (showAddDialog) {
                ShoppingItemDialog(
                        title = "Add to list",
                        onDismiss = { showAddDialog = false },
                        onConfirm = { name, qty ->
                                viewModel.addItem(name, qty)
                                showAddDialog = false
                        }
                )
        }

        // Edit dialog
        editingItem?.let { item ->
                ShoppingItemDialog(
                        title = "Edit item",
                        initialName = item.name,
                        initialQuantity = item.quantity,
                        onDismiss = { editingItem = null },
                        onConfirm = { name, qty ->
                                viewModel.updateItem(item.id, name, qty)
                                editingItem = null
                        }
                )
        }
}

@Composable
fun SwipeToDeleteShoppingItem(
        item: ShoppingListItem,
        membersMap: Map<String, String> = emptyMap(),
        onToggle: (Boolean) -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit = {}
) {
        var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
        val animatedOffset by
                animateFloatAsState(offsetX, animationSpec = tween(150), label = "swipe")

        // Optimistic local state — only keyed on id so Firestore's instant local-cache
        // snapshot doesn't immediately reinitialize and fight our optimistic flip.
        var isCheckedLocal by remember(item.id) { mutableStateOf(item.isChecked) }

        // Sync from Firebase when the server confirms (or reverts) the write.
        LaunchedEffect(item.isChecked) { isCheckedLocal = item.isChecked }

        val deleteThreshold = 400f // px to trigger instant delete on release

        Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                // Delete background
                Box(
                        modifier =
                                Modifier.matchParentSize()
                                        .background(
                                                color = Color(0xFFE57373).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                        ),
                        contentAlignment = Alignment.CenterEnd
                ) {
                        Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.padding(end = 20.dp).size(26.dp),
                                tint = MaterialTheme.colorScheme.error
                        )
                }

                // Foreground item — clickable to open edit dialog
                Surface(
                        modifier =
                                Modifier.offset { IntOffset(animatedOffset.roundToInt(), 0) }
                                        .fillMaxWidth()
                                        .clickable { onEdit() }
                                        .pointerInput(item.id) {
                                                detectHorizontalDragGestures(
                                                        onDragEnd = {
                                                                if (offsetX < -deleteThreshold) {
                                                                        onDelete()
                                                                } else {
                                                                        offsetX = 0f
                                                                }
                                                        },
                                                        onDragCancel = { offsetX = 0f },
                                                        onHorizontalDrag = { _, drag ->
                                                                offsetX =
                                                                        (offsetX + drag).coerceIn(
                                                                                -deleteThreshold *
                                                                                        1.1f,
                                                                                0f
                                                                        )
                                                        }
                                                )
                                        },
                        shape = RoundedCornerShape(10.dp),
                        color =
                                if (isCheckedLocal) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (isCheckedLocal) 0.dp else 1.dp
                ) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Checkbox
                                Checkbox(
                                        checked = isCheckedLocal,
                                        onCheckedChange = { _ ->
                                                val newValue = !isCheckedLocal
                                                isCheckedLocal = newValue
                                                onToggle(newValue)
                                        },
                                        colors =
                                                CheckboxDefaults.colors(
                                                        checkedColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        uncheckedColor =
                                                                MaterialTheme.colorScheme.primary
                                                )
                                )

                                Spacer(Modifier.width(6.dp))

                                // Name + user chip column
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = item.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style =
                                                        MaterialTheme.typography.bodyMedium.copy(
                                                                textDecoration =
                                                                        if (isCheckedLocal)
                                                                                TextDecoration
                                                                                        .LineThrough
                                                                        else null,
                                                                fontWeight =
                                                                        if (isCheckedLocal)
                                                                                FontWeight.Normal
                                                                        else FontWeight.Bold
                                                        ),
                                                color =
                                                        if (isCheckedLocal)
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant.copy(
                                                                        alpha = 0.5f
                                                                )
                                                        else MaterialTheme.colorScheme.onSurface
                                        )

                                        // User chip below the name
                                        if (item.addedByUserId.isNotBlank()) {
                                                val userName = membersMap[item.addedByUserId]
                                                if (userName != null) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                modifier =
                                                                        Modifier.padding(top = 2.dp)
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .Person,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        10.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Spacer(Modifier.width(3.dp))
                                                                Text(
                                                                        text = userName,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.7f
                                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Quantity badge (read-only on card)
                                if (item.quantity > 1) {
                                        Text(
                                                text = "×${item.quantity}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(4.dp))
                                }
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingItemDialog(
        title: String,
        initialName: String = "",
        initialQuantity: Int = 1,
        onDismiss: () -> Unit,
        onConfirm: (name: String, quantity: Int) -> Unit
) {
        var name by remember { mutableStateOf(initialName) }
        var qty by remember { mutableIntStateOf(initialQuantity.coerceAtLeast(1)) }
        var qtyText by remember { mutableStateOf(initialQuantity.coerceAtLeast(1).toString()) }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Name + quantity on the same row
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        OutlinedTextField(
                                                value = name,
                                                onValueChange = { name = it },
                                                label = { Text("Name") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                        )

                                        // Quantity control: − / field / +
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                        onClick = {
                                                                if (qty > 1) {
                                                                        qty--
                                                                        qtyText = qty.toString()
                                                                }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                ) {
                                                        Text(
                                                                "−",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        if (qty > 1)
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.3f
                                                                                        )
                                                        )
                                                }
                                                OutlinedTextField(
                                                        value = qtyText,
                                                        onValueChange = { v ->
                                                                qtyText = v.filter { it.isDigit() }
                                                                val n = qtyText.toIntOrNull() ?: 1
                                                                qty = n.coerceAtLeast(1)
                                                        },
                                                        singleLine = true,
                                                        keyboardOptions =
                                                                androidx.compose.foundation.text
                                                                        .KeyboardOptions(
                                                                                keyboardType =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .text
                                                                                                .input
                                                                                                .KeyboardType
                                                                                                .Number
                                                                        ),
                                                        modifier = Modifier.width(52.dp),
                                                        textStyle =
                                                                MaterialTheme.typography.bodyMedium
                                                                        .copy(
                                                                                textAlign =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .text
                                                                                                .style
                                                                                                .TextAlign
                                                                                                .Center
                                                                        )
                                                )
                                                IconButton(
                                                        onClick = {
                                                                qty++
                                                                qtyText = qty.toString()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                ) {
                                                        Text(
                                                                "+",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyLarge,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                }
                                        }
                                }
                        }
                },
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val finalQty = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        if (name.isNotBlank()) onConfirm(name.trim(), finalQty)
                                },
                                enabled = name.isNotBlank()
                        ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}
