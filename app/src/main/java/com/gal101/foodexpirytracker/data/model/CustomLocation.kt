package com.gal101.foodexpirytracker.data.model

data class CustomLocation(
        val id: String = "",
        val name: String = "",
        val iconName: String =
                "", // String name mapping to Material Icon (e.g., "kitchen", "ac_unit")
        val orderIndex: Int = 0
)
