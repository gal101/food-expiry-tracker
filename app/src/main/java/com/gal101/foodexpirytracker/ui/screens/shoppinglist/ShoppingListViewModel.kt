package com.gal101.foodexpirytracker.ui.screens.shoppinglist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gal101.foodexpirytracker.data.model.ShoppingListItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShoppingListViewModel(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) :
        ViewModel() {

    private val _items = MutableStateFlow<List<ShoppingListItem>>(emptyList())
    val items: StateFlow<List<ShoppingListItem>> = _items.asStateFlow()

    private var itemsListener: ListenerRegistration? = null
    private var currentHouseholdId: String? = null
    private var currentUserId: String = "" // set once from AppNavigation on login

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun startListeningToShoppingList(householdId: String) {
        if (currentHouseholdId == householdId) return

        stopListening()
        currentHouseholdId = householdId

        itemsListener =
                db.collection("households")
                        .document(householdId)
                        .collection("shoppingList")
                        .orderBy("addedAt")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.w("ShoppingListViewModel", "Listen failed.", error)
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val itemList =
                                        snapshot.documents.mapNotNull { doc ->
                                            doc.toObject(ShoppingListItem::class.java)
                                                    ?.copy(id = doc.id)
                                        }
                                _items.value = itemList
                            }
                        }
    }

    fun stopListening() {
        itemsListener?.remove()
        itemsListener = null
        currentHouseholdId = null
    }

    fun addItem(name: String, quantity: Int = 1) {
        val hId = currentHouseholdId ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val newItem =
                        mapOf(
                                "name" to name.trim(),
                                "isChecked" to false,
                                "addedByUserId" to currentUserId,
                                "quantity" to quantity,
                                "addedAt" to Timestamp.now()
                        )
                db.collection("households")
                        .document(hId)
                        .collection("shoppingList")
                        .add(newItem)
                        .await()
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error adding item", e)
            }
        }
    }

    fun updateItem(itemId: String, name: String, quantity: Int) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                db.collection("households")
                        .document(hId)
                        .collection("shoppingList")
                        .document(itemId)
                        .update(mapOf("name" to name.trim(), "quantity" to quantity))
                        .await()
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error updating item", e)
            }
        }
    }

    fun toggleItemChecked(itemId: String, isChecked: Boolean) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                db.collection("households")
                        .document(hId)
                        .collection("shoppingList")
                        .document(itemId)
                        .update("isChecked", isChecked)
                        .await()
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error updating item", e)
            }
        }
    }

    fun deleteItem(itemId: String) {
        val hId = currentHouseholdId ?: return
        viewModelScope.launch {
            try {
                db.collection("households")
                        .document(hId)
                        .collection("shoppingList")
                        .document(itemId)
                        .delete()
                        .await()
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error deleting item", e)
            }
        }
    }

    fun deleteCheckedItems() {
        val hId = currentHouseholdId ?: return
        val checkedItemIds = _items.value.filter { it.isChecked }.map { it.id }

        if (checkedItemIds.isEmpty()) return

        viewModelScope.launch {
            try {
                val batch = db.batch()
                checkedItemIds.forEach { itemId ->
                    val docRef =
                            db.collection("households")
                                    .document(hId)
                                    .collection("shoppingList")
                                    .document(itemId)
                    batch.delete(docRef)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("ShoppingListViewModel", "Error deleting checked items", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
