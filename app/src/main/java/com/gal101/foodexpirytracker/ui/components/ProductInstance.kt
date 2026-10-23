package com.gal101.foodexpirytracker.ui.components

import com.google.firebase.Timestamp
import java.util.UUID

data class ProductInstance(
    var id: String = UUID.randomUUID().toString(),
    var quantity: Double = 1.0,
    var expiryDate: Timestamp = Timestamp.now()
) {
    constructor() : this(UUID.randomUUID().toString(), 1.0, Timestamp.now())
}