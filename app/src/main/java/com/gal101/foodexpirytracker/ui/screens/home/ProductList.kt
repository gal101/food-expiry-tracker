package com.gal101.foodexpirytracker.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gal101.foodexpirytracker.ui.components.Product
import com.gal101.foodexpirytracker.ui.components.ProductInstance
import com.gal101.foodexpirytracker.ui.components.ProductsViewModel
import com.gal101.foodexpirytracker.ui.screens.shoppinglist.ShoppingListViewModel
import com.gal101.foodexpirytracker.utils.LocationIconMapper
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductList(
        viewModel: ProductsViewModel,
        shoppingListViewModel: ShoppingListViewModel,
        selectedLocations: List<String>,
        locations: List<com.gal101.foodexpirytracker.data.model.CustomLocation>
) {
        val context = LocalContext.current
        var isSortedByExpiry by rememberSaveable { mutableStateOf(false) }

        val allProducts by viewModel.products.collectAsState()

        val products =
                remember(allProducts, selectedLocations.toList(), isSortedByExpiry) {
                        // First filter by selectedLocations
                        val filteredProducts =
                                if (selectedLocations.isEmpty()) {
                                        emptyList()
                                } else {
                                        allProducts.filter { product ->
                                                // Match by exact ID or fallback to legacy exact
                                                // name match
                                                product.location in selectedLocations ||
                                                        locations.any { loc ->
                                                                loc.id in selectedLocations &&
                                                                        product.location.equals(
                                                                                loc.name,
                                                                                ignoreCase = true
                                                                        )
                                                        }
                                        }
                                }

                        // Then sort
                        if (!isSortedByExpiry) {
                                sortByNames(filteredProducts)
                        } else {
                                // Use the viewmodel's logic or a local sort. The ViewModel already
                                // provides
                                // `getProductsSortedByExpiry`
                                // but since we already collect `allProducts`, we can just sort
                                // locally to avoid
                                // flow switching overhead.
                                sortProductsLocallyByExpiry(filteredProducts)
                        }
                }

        val listState = rememberLazyListState()
        val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
        val visible = !isScrolling

        //    val isNearEnd by remember {
        //        derivedStateOf {
        //            val layoutInfo = listState.layoutInfo
        //            val totalItems = layoutInfo.totalItemsCount
        //            if (totalItems == 0) false
        //            else {
        //                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
        //                (lastVisibleItem?.index ?: 0) >= totalItems - 2
        //            }
        //        }
        //    }

        val bounce = remember { Animatable(0f) }

        LaunchedEffect(isSortedByExpiry) {
                bounce.animateTo(targetValue = 15f, animationSpec = tween(150))
                bounce.animateTo(targetValue = 0f, animationSpec = tween(200))
        }

        //
        //    LaunchedEffect(visible) {
        //        if (visible && isNearEnd) {
        //            listState.animateScrollToItem(products.lastIndex)
        //        }
        //    }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (products.isNotEmpty()) {
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Sort toggle (Left)
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                                Modifier.clickable {
                                                                isSortedByExpiry = !isSortedByExpiry
                                                        }
                                                        .padding(4.dp)
                                ) {
                                        Text(
                                                text = "Sort: ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                text = if (isSortedByExpiry) "Expiry" else "Name",
                                                style =
                                                        MaterialTheme.typography.labelLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                                imageVector =
                                                        androidx.compose.material.icons.Icons
                                                                .Default.run {
                                                                if (isSortedByExpiry)
                                                                        androidx.compose.material
                                                                                .icons.Icons.Default
                                                                                .DateRange
                                                                else
                                                                        androidx.compose.material
                                                                                .icons.Icons.Default
                                                                                .TextFields
                                                        },
                                                contentDescription = "Toggle Sort",
                                                modifier =
                                                        Modifier.size(16.dp).padding(start = 4.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }

                                // Counter (Right)
                                Text(
                                        text = "${products.sumOf { it.instances.size }} items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }

                        var expandedInstanceId by remember { mutableStateOf<String?>(null) }

                        LazyColumn(
                                state = listState,
                                modifier =
                                        Modifier.fillMaxSize().graphicsLayer {
                                                translationY = bounce.value
                                                alpha = 1f - (bounce.value * 4 / 100f)
                                        },
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                                itemsIndexed(
                                        products,
                                        key = { index, product -> "${product.documentId}-$index" }
                                ) { _, product ->
                                        ProductCard(
                                                product = product,
                                                locations = locations,
                                                expandedInstanceId = expandedInstanceId,
                                                onExpandInstance = { expandedInstanceId = it },
                                                onInstanceDelete = { instance ->
                                                        viewModel.deleteInstance(
                                                                product,
                                                                instance.id
                                                        )
                                                },
                                                onInstanceMove = { instance, newLocId ->
                                                        val newLocName =
                                                                locations
                                                                        .find { it.id == newLocId }
                                                                        ?.name
                                                                        ?: "Unknown"
                                                        viewModel.moveInstance(
                                                                product,
                                                                instance,
                                                                newLocId
                                                        )
                                                        Toast.makeText(
                                                                        context,
                                                                        "${product.name} moved to $newLocName",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                },
                                                onInstanceToShoppingListKeep = { instance ->
                                                        shoppingListViewModel.addItem(product.name)
                                                        Toast.makeText(
                                                                        context,
                                                                        "${product.name} added to cart",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                },
                                                onInstanceToShoppingListDelete = { instance ->
                                                        viewModel.deleteInstance(
                                                                product,
                                                                instance.id
                                                        )
                                                        shoppingListViewModel.addItem(product.name)
                                                        Toast.makeText(
                                                                        context,
                                                                        "${product.name} deleted and added to cart",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                },
                                                onInstanceClone = { instance ->
                                                        viewModel.cloneInstance(product, instance)
                                                        Toast.makeText(
                                                                        context,
                                                                        "${product.name} cloned",
                                                                        Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                },
                                                onInstanceEdit = {
                                                        instance,
                                                        newName,
                                                        newQty,
                                                        newExpiry ->
                                                        if (newName == product.name) {
                                                                viewModel.updateInstance(
                                                                        product,
                                                                        instance.id,
                                                                        newQty,
                                                                        newExpiry
                                                                )
                                                        } else {
                                                                // The UI EditInstanceDialog will
                                                                // handle the Rename All vs Move
                                                                // Only choose.
                                                                // But wait, the choice comes from
                                                                // the dialog. The lambda should
                                                                // receive the choice!
                                                        }
                                                },
                                                viewModel = viewModel
                                        )
                                }
                        }
                } else {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) {
                                val emptyText =
                                        if (selectedLocations.isEmpty()) "No location selected"
                                        else "No products added here!"
                                Text(emptyText, color = MaterialTheme.colorScheme.primary)
                        }
                }
        }
}

@Composable
fun ProductCard(
        product: Product,
        locations: List<com.gal101.foodexpirytracker.data.model.CustomLocation>,
        expandedInstanceId: String?,
        onExpandInstance: (String?) -> Unit,
        onInstanceDelete: (ProductInstance) -> Unit,
        onInstanceMove: (ProductInstance, String) -> Unit,
        onInstanceToShoppingListKeep: (ProductInstance) -> Unit,
        onInstanceToShoppingListDelete: (ProductInstance) -> Unit,
        onInstanceClone: (ProductInstance) -> Unit,
        onInstanceEdit: (ProductInstance, String, Double, com.google.firebase.Timestamp) -> Unit,
        viewModel: ProductsViewModel
) {
        Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
                val location = locations.find { it.id == product.location }
                val icon = LocationIconMapper.getIconFromName(location?.iconName)

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                        if (icon != null) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = "Location",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                                text = product.name,
                                style =
                                        MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                color = MaterialTheme.colorScheme.onBackground
                        )
                }

                Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                ) {
                        product.instances.forEach { instance ->
                                key(instance.id) {
                                        var visible by remember { mutableStateOf(true) }
                                        var performDelete by remember { mutableStateOf(false) }
                                        var performShoppingList by remember {
                                                mutableStateOf(false)
                                        }

                                        AnimatedVisibility(
                                                visible = visible,
                                                exit =
                                                        slideOutHorizontally(
                                                                animationSpec = tween(200)
                                                        ) { -it } +
                                                                fadeOut(
                                                                        animationSpec = tween(200)
                                                                ) +
                                                                shrinkVertically(
                                                                        animationSpec = tween(200)
                                                                ),
                                                enter =
                                                        slideInVertically() +
                                                                fadeIn() +
                                                                expandVertically()
                                        ) {
                                                ProductInstanceCard(
                                                        product = product,
                                                        locations = locations,
                                                        instance = instance,
                                                        expanded =
                                                                expandedInstanceId == instance.id,
                                                        onExpand = {
                                                                onExpandInstance(
                                                                        if (expandedInstanceId ==
                                                                                        instance.id
                                                                        )
                                                                                null
                                                                        else instance.id
                                                                )
                                                        },
                                                        onDelete = {
                                                                onExpandInstance(null)
                                                                performDelete = true
                                                                visible = false
                                                        },
                                                        onToShoppingListKeep = {
                                                                onInstanceToShoppingListKeep(
                                                                        instance
                                                                )
                                                                onExpandInstance(null)
                                                        },
                                                        onToShoppingListDelete = {
                                                                onExpandInstance(null)
                                                                performShoppingList = true
                                                                visible = false
                                                        },
                                                        onMove = { newLocId ->
                                                                onInstanceMove(instance, newLocId)
                                                                onExpandInstance(null)
                                                        },
                                                        onClone = {
                                                                onInstanceClone(instance)
                                                                onExpandInstance(null)
                                                        },
                                                        viewModel = viewModel
                                                )
                                        }

                                        LaunchedEffect(visible) {
                                                if (!visible) {
                                                        kotlinx.coroutines.delay(200)
                                                        if (performDelete)
                                                                onInstanceDelete(instance)
                                                        if (performShoppingList)
                                                                onInstanceToShoppingListDelete(
                                                                        instance
                                                                )
                                                }
                                        }
                                }
                        }
                }
        }
}

// ---------------------- quantity formatting -----------------------

/**
 * Formats a quantity value according to the unit: ml/g → integer, kg/L → 1 decimal, everything else
 * → integer.
 */
fun formatQty(qty: Double, unit: String): String {
        val intUnits = setOf("ml", "g", "unit", "pcs")
        return if (unit.lowercase() in intUnits) "${qty.toInt()} $unit"
        else "${String.format("%.1f", qty)} $unit"
}

// ---------------------- ProductInstanceCard ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductInstanceCard(
        product: Product,
        locations: List<com.gal101.foodexpirytracker.data.model.CustomLocation>,
        instance: ProductInstance,
        expanded: Boolean,
        onExpand: () -> Unit,
        onDelete: () -> Unit,
        onToShoppingListKeep: () -> Unit,
        onToShoppingListDelete: () -> Unit,
        onMove: (String) -> Unit,
        onClone: () -> Unit,
        warningThresholdDays: Int = 7,
        viewModel: ProductsViewModel
) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val tag =
                remember(product, instance) {
                        getExpiryTag(product, instance, warningThresholdDays)
                }
        var showMoveMenu by remember { mutableStateOf(false) }
        var showInfoDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
                // ── Foreground Card (clickable, no swipe) ────────────────────────
                Card(
                        modifier =
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                        onExpand()
                                },
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                // Vertical color bar
                                Box(
                                        modifier =
                                                Modifier.fillMaxHeight()
                                                        .width(6.dp)
                                                        .background(
                                                                tag?.background ?: Color.Transparent
                                                        )
                                )
                                // Card content
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .fillMaxHeight()
                                                        .padding(
                                                                start = 12.dp,
                                                                top = 8.dp,
                                                                bottom = 8.dp,
                                                                end = 16.dp
                                                        )
                                ) {
                                        val daysRemaining =
                                                ChronoUnit.DAYS.between(
                                                        LocalDate.now(),
                                                        instance.expiryDate
                                                                .toDate()
                                                                .toInstant()
                                                                .atZone(ZoneId.systemDefault())
                                                                .toLocalDate()
                                                )
                                        val hasWarning = daysRemaining in 0..warningThresholdDays
                                        val qtyStr = formatQty(instance.quantity, product.unit)

                                        if (hasWarning) {
                                                Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        androidx.compose.material
                                                                                .icons.Icons.Default
                                                                                .Info,
                                                                        contentDescription =
                                                                                "Warning",
                                                                        tint = Color(0xFFE91E63),
                                                                        modifier =
                                                                                Modifier.size(14.dp)
                                                                )
                                                                Spacer(Modifier.width(3.dp))
                                                                Text(
                                                                        text =
                                                                                if (daysRemaining ==
                                                                                                0L
                                                                                )
                                                                                        "Expires today!"
                                                                                else
                                                                                        "Expiring soon",
                                                                        color = Color(0xFFE91E63),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        )
                                                                )
                                                        }
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween,
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = qtyStr,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                )
                                                                ExpiryPill(tag)
                                                        }
                                                }
                                        } else {
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.CenterStart
                                                ) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween,
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Text(
                                                                        text = qtyStr,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium
                                                                                        .copy(
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold
                                                                                        ),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                )
                                                                ExpiryPill(tag)
                                                        }
                                                }
                                        }
                                }
                        }
                }

                // "Action row” visible only when this card is expanded
                //
                AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
                ) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // 1. Shopping Cart button (KEEP instances)
                                FilledTonalIconButton(
                                        onClick = { onToShoppingListKeep() },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.AddShoppingCart,
                                                contentDescription = "Add to shopping list (keep)",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }

                                // Move button + DropdownMenu
                                Box {
                                        FilledTonalIconButton(
                                                onClick = { showMoveMenu = true },
                                                modifier = Modifier.size(40.dp),
                                                colors =
                                                        IconButtonDefaults
                                                                .filledTonalIconButtonColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                )
                                        ) {
                                                Icon(
                                                        androidx.compose.material.icons.Icons
                                                                .Default.MoveDown,
                                                        contentDescription = "Move",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                        DropdownMenu(
                                                expanded = showMoveMenu,
                                                onDismissRequest = { showMoveMenu = false }
                                        ) {
                                                locations.forEach { loc ->
                                                        val locIcon =
                                                                com.gal101.foodexpirytracker.utils
                                                                        .LocationIconMapper
                                                                        .getIconFromName(
                                                                                loc.iconName
                                                                        )
                                                        DropdownMenuItem(
                                                                text = { Text(loc.name) },
                                                                leadingIcon = {
                                                                        if (locIcon != null)
                                                                                Icon(
                                                                                        locIcon,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        20.dp
                                                                                                ),
                                                                                        tint =
                                                                                                if (loc.id ==
                                                                                                                product.location
                                                                                                )
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                else
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .onSurfaceVariant
                                                                                )
                                                                },
                                                                onClick = {
                                                                        showMoveMenu = false
                                                                        if (loc.id !=
                                                                                        product.location
                                                                        )
                                                                                onMove(loc.id)
                                                                },
                                                                colors =
                                                                        MenuDefaults.itemColors(
                                                                                textColor =
                                                                                        if (loc.id ==
                                                                                                        product.location
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                        )
                                                        )
                                                }
                                        }
                                }

                                // Clone button
                                FilledTonalIconButton(
                                        onClick = { onClone() },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Clone",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }

                                var showEditDialog by remember { mutableStateOf(false) }

                                // Edit button
                                FilledTonalIconButton(
                                        onClick = { showEditDialog = true },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }

                                if (showEditDialog) {
                                        EditInstanceDialog(
                                                product = product,
                                                instance = instance,
                                                onDismiss = { showEditDialog = false },
                                                viewModel = viewModel,
                                                onSaveSuccess = { showEditDialog = false }
                                        )
                                }

                                // Info button
                                FilledTonalIconButton(
                                        onClick = { showInfoDialog = true },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Info",
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }

                                // Shopping Cart button (DELETE instances)
                                FilledTonalIconButton(
                                        onClick = { onToShoppingListDelete() },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                Color(0xFFE57373).copy(alpha = 0.2f)
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.ShoppingCartCheckout,
                                                contentDescription =
                                                        "Add to shopping list (delete)",
                                                modifier = Modifier.size(20.dp),
                                                tint = Color(0xFFE57373)
                                        )
                                }

                                // Delete button
                                FilledTonalIconButton(
                                        onClick = { onDelete() },
                                        modifier = Modifier.size(40.dp),
                                        colors =
                                                IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor =
                                                                Color(0xFFE57373).copy(alpha = 0.2f)
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFE57373),
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }
                }
        }

        // â”€â”€ Info dialog
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (showInfoDialog) {
                val locationName =
                        locations.find { it.id == product.location }?.name ?: product.location
                val localDate =
                        instance.expiryDate
                                .toDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                AlertDialog(
                        onDismissRequest = { showInfoDialog = false },
                        title = { Text("Product Info") },
                        text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        @Composable
                                        fun Row(label: String, value: String?) {
                                                if (!value.isNullOrBlank()) {
                                                        androidx.compose.foundation.layout.Row(
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(6.dp)
                                                        ) {
                                                                Text(
                                                                        "$label:",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        90.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        value,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall
                                                                )
                                                        }
                                                }
                                        }
                                        Row("Name", product.name)
                                        Row("Brand", product.brands)
                                        Row("Generic", product.genericName)
                                        Row("Barcode", product.barcode)
                                        Row("Unit", product.unit)
                                        Row("Quantity", formatQty(instance.quantity, product.unit))
                                        Row("Expiry", localDate.format(dateFormatter))
                                        Row("Location", locationName)
                                        if (!product.imageUrl.isNullOrBlank()) {
                                                Spacer(Modifier.height(8.dp))
                                                coil.compose.AsyncImage(
                                                        model = product.imageUrl,
                                                        contentDescription = "Product image",
                                                        contentScale =
                                                                androidx.compose.ui.layout
                                                                        .ContentScale.Fit,
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(160.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        )
                                                                        .background(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                        )
                                                )
                                        }
                                }
                        },
                        confirmButton = {
                                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
                        }
                )
        }
}

fun sortProductsLocallyByExpiry(allProducts: List<Product>): List<Product> {
        val allInstances =
                allProducts.flatMap { product ->
                        product.instances.map { instance -> product to instance }
                }

        val sortedInstances = allInstances.sortedBy { (_, instance) -> instance.expiryDate }

        val groupedList = mutableListOf<Product>()
        var currentProductGroup: Product? = null

        for ((productInfo, instance) in sortedInstances) {
                if (currentProductGroup == null ||
                                currentProductGroup.documentId != productInfo.documentId
                ) {
                        currentProductGroup?.let { groupedList.add(it) }
                        currentProductGroup = productInfo.copy(instances = mutableListOf(instance))
                } else {
                        currentProductGroup.instances.add(instance)
                }
        }

        currentProductGroup?.let { groupedList.add(it) }

        return groupedList
}

fun sortByNames(products: List<Product>): List<Product> {
        return products.map { product ->
                val sortedInstances = product.instances.sortedBy { it.expiryDate }
                product.copy(instances = sortedInstances.toMutableList())
        }
}

@Composable
fun ExpiryTagPill(tag: ExpiryTag) {
        Surface(
                shape = RoundedCornerShape(percent = 50),
                color = tag.background,
                contentColor = tag.textColor,
        ) {
                Text(
                        text = tag.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                )
        }
}

/** "Exp. " in normal weight + the date value in bold. Color is always onSurface (white in dark). */
@Composable
fun ExpiryDateText(dateString: String, tag: ExpiryTag?, hasWarning: Boolean) {
        androidx.compose.ui.text.buildAnnotatedString {
                // rendered via BasicText for mixed styles
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = "Exp. ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = dateString,
                        style =
                                MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                ),
                        color = MaterialTheme.colorScheme.onSurface
                )
        }
}

/** Compact pill badge showing the expiry tag text. */
@Composable
fun ExpiryPill(tag: ExpiryTag?) {
        if (tag != null) {
                Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = tag.background.copy(alpha = 0.2f),
                ) {
                        Text(
                                text = tag.text,
                                color = tag.background,
                                style =
                                        MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                        ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInstanceDialog(
        product: Product,
        instance: ProductInstance,
        onDismiss: () -> Unit,
        viewModel: ProductsViewModel,
        onSaveSuccess: () -> Unit
) {
        var editedName by remember { mutableStateOf(product.name) }
        var editedQty by remember { mutableStateOf(instance.quantity.toString()) }

        // Date handling
        val initialLocalDate =
                instance.expiryDate
                        .toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        var editedLocalDate by remember { mutableStateOf(initialLocalDate) }
        var showDatePicker by remember { mutableStateOf(false) }

        val datePickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis =
                                initialLocalDate
                                        .atStartOfDay(java.time.ZoneOffset.UTC)
                                        .toInstant()
                                        .toEpochMilli()
                )

        var showRenameConfirmDialog by remember { mutableStateOf(false) }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Product Instance") },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        label = { Text("Product Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                )
                                OutlinedTextField(
                                        value = editedQty,
                                        onValueChange = { editedQty = it },
                                        label = { Text("Quantity (${product.unit})") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions =
                                                androidx.compose.foundation.text.KeyboardOptions(
                                                        keyboardType =
                                                                androidx.compose.ui.text.input
                                                                        .KeyboardType.Decimal
                                                )
                                )

                                // Date Picker row
                                val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                                OutlinedCard(
                                        onClick = { showDatePicker = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.outlinedCardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Default.DateRange,
                                                                contentDescription = null,
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                                "Expiry Date",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium
                                                        )
                                                }
                                                Text(
                                                        editedLocalDate.format(dateFormatter),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                        }
                },
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val finalQty =
                                                editedQty.replace(",", ".").toDoubleOrNull()
                                                        ?: instance.quantity
                                        val finalTimestamp =
                                                com.google.firebase.Timestamp(
                                                        java.util.Date.from(
                                                                editedLocalDate
                                                                        .atStartOfDay(
                                                                                ZoneId.systemDefault()
                                                                        )
                                                                        .toInstant()
                                                        )
                                                )

                                        val cleanName = editedName.trim()
                                        if (cleanName.equals(product.name.trim(), ignoreCase = true)
                                        ) {
                                                // Name did not change: just update instance
                                                viewModel.updateInstance(
                                                        product,
                                                        instance.id,
                                                        finalQty,
                                                        finalTimestamp
                                                )
                                                onSaveSuccess()
                                        } else {
                                                // Name changed: ask the user what to do
                                                showRenameConfirmDialog = true
                                        }
                                }
                        ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )

        // Nested DatePicker Dialog
        if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                        editedLocalDate =
                                                                java.time.Instant.ofEpochMilli(it)
                                                                        .atZone(
                                                                                java.time.ZoneOffset
                                                                                        .UTC
                                                                        )
                                                                        .toLocalDate()
                                                }
                                                showDatePicker = false
                                        }
                                ) { Text("OK") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                ) { DatePicker(state = datePickerState) }
        }

        // Nested Rename Confirmation Dialog
        if (showRenameConfirmDialog) {
                AlertDialog(
                        onDismissRequest = { showRenameConfirmDialog = false },
                        title = { Text("Rename Product") },
                        text = {
                                Text(
                                        "Do you want to rename all instances of '${product.name}' exactly to '$editedName', or move only this edited instance to '$editedName'?"
                                )
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                val finalQty =
                                                        editedQty.replace(",", ".").toDoubleOrNull()
                                                                ?: instance.quantity
                                                val finalTimestamp =
                                                        com.google.firebase.Timestamp(
                                                                java.util.Date.from(
                                                                        editedLocalDate
                                                                                .atStartOfDay(
                                                                                        ZoneId.systemDefault()
                                                                                )
                                                                                .toInstant()
                                                                )
                                                        )
                                                viewModel.renameProductAndUpdateInstance(
                                                        product,
                                                        instance.id,
                                                        editedName,
                                                        finalQty,
                                                        finalTimestamp
                                                )
                                                showRenameConfirmDialog = false
                                                onSaveSuccess()
                                        }
                                ) { Text("Rename All") }
                        },
                        dismissButton = {
                                TextButton(
                                        onClick = {
                                                val finalQty =
                                                        editedQty.replace(",", ".").toDoubleOrNull()
                                                                ?: instance.quantity
                                                val finalTimestamp =
                                                        com.google.firebase.Timestamp(
                                                                java.util.Date.from(
                                                                        editedLocalDate
                                                                                .atStartOfDay(
                                                                                        ZoneId.systemDefault()
                                                                                )
                                                                                .toInstant()
                                                                )
                                                        )
                                                viewModel.moveInstanceToNewName(
                                                        product,
                                                        instance.id,
                                                        editedName,
                                                        finalQty,
                                                        finalTimestamp
                                                )
                                                showRenameConfirmDialog = false
                                                onSaveSuccess()
                                        }
                                ) { Text("Move Only This") }
                        }
                )
        }
}
