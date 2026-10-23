package com.gal101.foodexpirytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gal101.foodexpirytracker.ui.theme.FoodExpiryTrackerTheme
import com.gal101.foodexpirytracker.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodExpiryTrackerTheme {
                AppNavigation()  // 🧭 Aici încărcăm tot sistemul de navigație Compose
            }
        }
    }
}