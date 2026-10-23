package com.gal101.foodexpirytracker

// ProductDataStore.kt

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.productDataStore by preferencesDataStore(name = "product_names")

