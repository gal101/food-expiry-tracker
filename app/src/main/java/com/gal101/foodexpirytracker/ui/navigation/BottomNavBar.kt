package com.gal101.foodexpirytracker.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gal101.foodexpirytracker.R

/** Supports both vector icons (from Icons.*) and drawable resource icons (from res/drawable). */
sealed class NavIcon {
        data class Vector(val imageVector: ImageVector) : NavIcon()
        data class Drawable(@DrawableRes val resId: Int) : NavIcon()
}

data class BottomNavItem(val route: String, val label: String, val icon: NavIcon)

@Composable
fun BottomNavBar(navController: NavController) {
        val items =
                listOf(
                        BottomNavItem("my_products", "Home", NavIcon.Vector(Icons.Default.Home)),
                        BottomNavItem(
                                "scan",
                                "Scan",
                                NavIcon.Drawable(R.drawable.barcode_scanner)
                        ),
                        BottomNavItem(
                                "shopping_list",
                                "Shopping",
                                NavIcon.Vector(Icons.Default.Checklist)
                        ),
                        BottomNavItem("recipes", "Recipes", NavIcon.Vector(Icons.Default.MenuBook))
                )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                items.forEach { item ->
                        NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                        navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                        }
                                },
                                icon = {
                                        when (val ic = item.icon) {
                                                is NavIcon.Vector ->
                                                        Icon(
                                                                ic.imageVector,
                                                                contentDescription = item.label
                                                        )
                                                is NavIcon.Drawable ->
                                                        Icon(
                                                                painterResource(ic.resId),
                                                                contentDescription = item.label
                                                        )
                                        }
                                },
                                label = { Text(item.label) },
                                colors =
                                        NavigationBarItemDefaults.colors(
                                                selectedIconColor =
                                                        MaterialTheme.colorScheme.primary,
                                                selectedTextColor =
                                                        MaterialTheme.colorScheme.primary,
                                                indicatorColor =
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.2f
                                                        ),
                                                unselectedIconColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                unselectedTextColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                        )
                        )
                }
        }
}
