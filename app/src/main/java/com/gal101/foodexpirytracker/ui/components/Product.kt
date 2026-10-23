package com.gal101.foodexpirytracker.ui.components

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
        @DocumentId val documentId: String = "",
        val name: String = "",
        val location: String = "",
        val unit: String = "",
        val instances: MutableList<ProductInstance> = mutableListOf(),
        // OFF metadata — nullable, present only for scanned products
        val brands: String? = null,
        val genericName: String? = null,
        val imageUrl: String? = null,
        val barcode: String? = null
) {

    companion object {
        /**
         * Creează un produs nou fără instanțe definite încă. Vei adăuga instanțele ulterior cu
         * addInstance().
         */
        fun createEmpty(name: String, location: String, unit: String): Product {
            return Product(
                    name = name,
                    location = location,
                    unit = unit,
                    instances = mutableListOf()
            )
        }

        fun createWithInstances(
                name: String,
                location: String,
                unit: String,
                instances: MutableList<ProductInstance>,
                brands: String? = null,
                genericName: String? = null,
                imageUrl: String? = null,
                barcode: String? = null
        ): Product {
            return Product(
                    name = name,
                    location = location,
                    unit = unit,
                    instances = instances,
                    brands = brands,
                    genericName = genericName,
                    imageUrl = imageUrl,
                    barcode = barcode
            )
        }

        /** Creează un produs cu un număr fix de instanțe generate automat. */
        //        fun createWithDefaults(
        //            name: String,
        //            location: StorageLocation,
        //            unit: String,
        //            count: Int
        //        ): Product {
        //            val instances = MutableList(count) {
        //                ProductInstance(
        //                    quantity = 1.0,
        //                    expiryDate = LocalDate.now().plusDays(7)
        //                )
        //            }
        //            return Product(
        //                name = name,
        //                location = location,
        //                unit = unit,
        //                instances = instances
        //            )
        //        }
    }

    fun addInstance(quantity: Double, expiryDate: Timestamp) {
        instances.add(
                ProductInstance(
                        id = instances.size.toString(),
                        quantity = quantity,
                        expiryDate = expiryDate
                )
        )
    }

    fun removeInstance(instanceId: String) {
        instances.removeIf { it.id == instanceId }
    }
}
