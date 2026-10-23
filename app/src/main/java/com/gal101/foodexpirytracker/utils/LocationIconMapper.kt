package com.gal101.foodexpirytracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object LocationIconMapper {
    val predefinedIcons =
            listOf(
                    "kitchen" to Icons.Default.Kitchen,
                    "ac_unit" to Icons.Default.AcUnit,
                    "inventory" to Icons.Default.Inventory,
                    "list" to Icons.Default.List,
                    "shopping_cart" to Icons.Default.ShoppingCart,
                    "home" to Icons.Default.Home,
                    "business" to Icons.Default.Business,
                    "star" to Icons.Default.Star
            )

    fun getIconFromName(name: String?): ImageVector {
        if (name.isNullOrBlank()) return Icons.Default.Kitchen
        return predefinedIcons.find { it.first == name }?.second ?: Icons.Default.Kitchen
    }
}
