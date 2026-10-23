package com.gal101.foodexpirytracker.ui.screens.manage_locations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gal101.foodexpirytracker.data.model.CustomLocation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationsViewModel(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) :
        ViewModel() {

    private val _locations = MutableStateFlow<List<CustomLocation>>(emptyList())
    val locations: StateFlow<List<CustomLocation>> = _locations.asStateFlow()

    private var locationsListener: ListenerRegistration? = null
    private var currentHouseholdId: String? = null

    fun startListeningToLocations(householdId: String) {
        if (currentHouseholdId == householdId) return // Already listening to this household

        stopListening()
        currentHouseholdId = householdId

        locationsListener =
                db.collection("households")
                        .document(householdId)
                        .collection("locations")
                        .orderBy("orderIndex")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("LocationsViewModel", "Listen failed.", error)
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val locationList =
                                        snapshot.documents.mapNotNull { doc ->
                                            doc.toObject(CustomLocation::class.java)
                                                    ?.copy(id = doc.id)
                                        }
                                _locations.value = locationList

                                // Check if we need to seed
                                if (snapshot.isEmpty) {
                                    checkAndSeedDefaultLocations(householdId)
                                }
                            }
                        }
    }

    private fun checkAndSeedDefaultLocations(householdId: String) {
        viewModelScope.launch {
            try {
                val householdRef = db.collection("households").document(householdId)
                val doc = householdRef.get().await()

                if (!doc.exists()) {
                    Log.w(
                            "LocationsViewModel",
                            "Household $householdId does not exist. Aborting seed."
                    )
                    return@launch
                }

                val hasSeeded = doc.getBoolean("hasSeededLocations") ?: false
                if (!hasSeeded) {
                    val batch = db.batch()

                    val defaults =
                            listOf(
                                    mapOf(
                                            "name" to "Fridge",
                                            "iconName" to "kitchen",
                                            "orderIndex" to 0
                                    ),
                                    mapOf(
                                            "name" to "Freezer",
                                            "iconName" to "ac_unit",
                                            "orderIndex" to 1
                                    ),
                                    mapOf(
                                            "name" to "Pantry",
                                            "iconName" to "shelves",
                                            "orderIndex" to 2
                                    ),
                                    mapOf(
                                            "name" to "Non-food",
                                            "iconName" to "category",
                                            "orderIndex" to 3
                                    )
                            )

                    defaults.forEach { loc ->
                        val newRef = householdRef.collection("locations").document()
                        batch.set(newRef, loc)
                    }

                    batch.update(householdRef, "hasSeededLocations", true)
                    batch.commit().await()
                }
            } catch (e: Exception) {
                Log.e("LocationsViewModel", "Failed to seed default locations", e)
            }
        }
    }

    fun stopListening() {
        locationsListener?.remove()
        locationsListener = null
        currentHouseholdId = null
    }

    fun addLocation(name: String, iconName: String) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                val currentSize = _locations.value.size
                val newLocation =
                        mapOf(
                                "name" to name.trim(),
                                "iconName" to iconName,
                                "orderIndex" to currentSize
                        )
                db.collection("households")
                        .document(hId)
                        .collection("locations")
                        .add(newLocation)
                        .await()
            } catch (e: Exception) {
                Log.e("LocationsViewModel", "Error adding location", e)
            }
        }
    }

    fun updateLocation(locationId: String, name: String, iconName: String) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                db.collection("households")
                        .document(hId)
                        .collection("locations")
                        .document(locationId)
                        .update(mapOf("name" to name.trim(), "iconName" to iconName))
                        .await()
            } catch (e: Exception) {
                Log.e("LocationsViewModel", "Error updating location", e)
            }
        }
    }

    fun deleteLocation(locationId: String) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                // Cascading delete: find all products with this locationId
                val productsRef = db.collection("households").document(hId).collection("products")

                val productsSnapshot =
                        productsRef.whereEqualTo("location", locationId).get().await()

                // Use a batch to delete the location and all associated products
                val batch = db.batch()

                // Add product deletions to batch
                for (doc in productsSnapshot.documents) {
                    batch.delete(doc.reference)
                }

                // Add location deletion to batch
                val locationRef =
                        db.collection("households")
                                .document(hId)
                                .collection("locations")
                                .document(locationId)
                batch.delete(locationRef)

                // Commit everything atomically
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("LocationsViewModel", "Error deleting location and cascading products", e)
            }
        }
    }

    // Stub for future drag-and-drop reordering
    fun reorderLocations() {
        // ... bulk update orderIndex for all documents
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
