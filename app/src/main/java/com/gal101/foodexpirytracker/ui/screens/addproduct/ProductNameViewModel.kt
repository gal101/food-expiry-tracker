package com.gal101.foodexpirytracker.ui.screens.addproduct

// Separator remains the same
// ... imports remain the same

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal const val NAME_UNIT_SEPARATOR = "|"

data class ProductTemplate(
        val name: String,
        val unit: String,
        val location: String,
        val quantity: Double
)

class ProductNameViewModel() : ViewModel() {
    private val _templates = MutableStateFlow<List<ProductTemplate>>(emptyList())
    val templates: StateFlow<List<ProductTemplate>> = _templates

    private val KEY_NAMES = stringSetPreferencesKey("product_names_key")
    private var dataStore: DataStore<Preferences>? = null

    fun initDataStore(ds: DataStore<Preferences>) {
        if (dataStore != null) return
        dataStore = ds
        viewModelScope.launch {
            val namesFromStore = dataStore!!.data.map { it[KEY_NAMES] }.first()

            if (namesFromStore == null) {
                val defaultTemplates =
                        setOf(
                                "Milk${NAME_UNIT_SEPARATOR}L${NAME_UNIT_SEPARATOR}Fridge${NAME_UNIT_SEPARATOR}1.0",
                                "Eggs${NAME_UNIT_SEPARATOR}Unit${NAME_UNIT_SEPARATOR}Fridge${NAME_UNIT_SEPARATOR}10.0",
                                "Flour${NAME_UNIT_SEPARATOR}kg${NAME_UNIT_SEPARATOR}Pantry${NAME_UNIT_SEPARATOR}1.0"
                        )
                dataStore!!.edit { prefs -> prefs[KEY_NAMES] = defaultTemplates }
                _templates.value = parseTemplates(defaultTemplates)
                Log.d("ProductNameVM", "Initialized with default templates.")
            } else {
                _templates.value = parseTemplates(namesFromStore)
                Log.d("ProductNameVM", "Loaded templates from DataStore.")
            }
        }
    }

    private fun parseTemplates(templateSet: Set<String>): List<ProductTemplate> {
        return templateSet
                .mapNotNull { combined ->
                    val parts = combined.split(NAME_UNIT_SEPARATOR)
                    if (parts.size == 4) {
                        try {
                            val location = parts[2]
                            val quantity = parts[3].toDouble()
                            ProductTemplate(parts[0], parts[1], location, quantity)
                        } catch (e: Exception) {
                            Log.e(
                                    "ProductNameVM",
                                    "Invalid format in stored template: $combined",
                                    e
                            )
                            null
                        }
                    } else {
                        // Ignore old 3-part templates (effective removal from UI)
                        Log.w("ProductNameVM", "Ignoring invalid/old template format: $combined")
                        null
                    }
                }
                .sortedBy { it.name }
    }

    fun addTemplate(name: String, unit: String, location: String, quantity: Double) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank() || trimmedName.contains(NAME_UNIT_SEPARATOR)) {
            Log.w("ProductNameVM", "Invalid name format: $trimmedName")
            return
        }

        val combined =
                "$trimmedName$NAME_UNIT_SEPARATOR$unit$NAME_UNIT_SEPARATOR$location$NAME_UNIT_SEPARATOR$quantity"

        val currentTemplates = _templates.value
        if (currentTemplates.any {
                    it.name.equals(trimmedName, ignoreCase = true) &&
                            it.unit == unit &&
                            it.location == location &&
                            it.quantity == quantity
                }
        ) {
            Log.d("ProductNameVM", "Template already exists: $combined")
            return
        }

        _templates.value =
                (currentTemplates + ProductTemplate(trimmedName, unit, location, quantity))
                        .sortedBy { it.name }

        viewModelScope.launch {
            dataStore?.edit { prefs ->
                val currentSet = prefs[KEY_NAMES] ?: emptySet()
                // Filter out old formats if we want to clean up disk, OR just append new one.
                // User said "remove old data from disk".
                // To do that, we should probably rewrite the SET with only valid templates?
                // For now, appending is safer, but let's just add the valid one.
                // Actually, if we want to "remove" old ones, we should save only the parsed (valid)
                // ones + the new one.
                // But parseTemplates only returns valid ones.
                // So let's re-serialize _templates.value!
                // That ensures the disk mirrors the clean in-memory state.

                val newSet =
                        _templates
                                .value
                                .map {
                                    "${it.name}$NAME_UNIT_SEPARATOR${it.unit}$NAME_UNIT_SEPARATOR${it.location}$NAME_UNIT_SEPARATOR${it.quantity}"
                                }
                                .toSet()

                prefs[KEY_NAMES] = newSet
                Log.d("ProductNameVM", "Saved templates to DataStore.")
            }
        }
    }

    fun removeTemplate(template: ProductTemplate) {
        val currentTemplates = _templates.value.toMutableList()
        if (currentTemplates.remove(template)) {
            _templates.value = currentTemplates.sortedBy { it.name }

            viewModelScope.launch {
                dataStore?.edit { prefs ->
                    val newSet =
                            _templates
                                    .value
                                    .map {
                                        "${it.name}$NAME_UNIT_SEPARATOR${it.unit}$NAME_UNIT_SEPARATOR${it.location}$NAME_UNIT_SEPARATOR${it.quantity}"
                                    }
                                    .toSet()
                    prefs[KEY_NAMES] = newSet
                    Log.d("ProductNameVM", "Removed template from DataStore.")
                }
            }
        }
    }
}
