package com.gal101.foodexpirytracker.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ShoppingListItem(
        val id: String = "",
        val name: String = "",
        @get:PropertyName("isChecked")
        @set:PropertyName("isChecked")
        var isChecked: Boolean = false,
        @get:PropertyName("addedByUserId")
        @set:PropertyName("addedByUserId")
        var addedByUserId: String = "",
        @get:PropertyName("quantity") @set:PropertyName("quantity") var quantity: Int = 1,
        val addedAt: Timestamp = Timestamp.now()
)
