package com.gal101.foodexpirytracker.ui.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductsViewModel : ViewModel() {
    // lista observabilă
    val products = MutableStateFlow<List<Product>>(emptyList())

    private val db = Firebase.firestore
    private var currentHouseholdId: String? = null
    private var productListener: ListenerRegistration? = null

    fun getProductsListFromFirebase(householdId: String) {
        // If we are already listening to this household, do nothing
        if (householdId == currentHouseholdId) return

        // Stop listening to the old household's products
        productListener?.remove()

        // Store the new household ID
        currentHouseholdId = householdId

        // Create a query to the 'products' sub-collection
        val productsRef = db.collection("households").document(householdId).collection("products")

        // Start a real-time listener
        productListener =
                productsRef.addSnapshotListener { snapshot, error ->
                    // Handle any errors
                    if (error != null) {
                        Log.w("ProductsVM", "Listen failed.", error)
                        products.value = emptyList() // Clear list on error
                        return@addSnapshotListener
                    }

                    // If the snapshot is valid, convert the documents to Product objects
                    if (snapshot != null) {
                        val productList = snapshot.toObjects(Product::class.java)
                        products.value = productList
                        Log.d("ProductsVM", "Loaded ${productList.size} products from Firestore.")
                    }
                }
    }

    fun addProduct(product: Product) {
        products.value = products.value + product
    }

    fun deleteInstance(product: Product, instanceID: String) {
        // We need the household ID to know where to write
        val householdId = currentHouseholdId
        if (householdId == null) {
            Log.e("ProductsVM", "Cannot delete, householdId is null")
            return
        }

        // We need the product's document ID to find it
        if (product.documentId.isEmpty()) {
            Log.e("ProductsVM", "Cannot delete, product document ID is missing")
            return
        }

        // Get the reference to the specific product document
        val productDocRef =
                db.collection("households")
                        .document(householdId)
                        .collection("products")
                        .document(product.documentId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Run the deletion inside a database transaction
                db
                        .runTransaction { transaction ->
                            // 1. Read: Get the *current* state of the document from the server,
                            // not from the local cache.
                            val snapshot = transaction.get(productDocRef)
                            val currentProduct = snapshot.toObject<Product>()

                            if (currentProduct == null) {
                                // The product was already deleted by someone else,
                                // so our job is done. We stop the transaction.
                                Log.w(
                                        "ProductsVM",
                                        "Product not found during transaction, already deleted."
                                )
                                return@runTransaction
                            }

                            // 2. Modify: Create the new list in memory, based on the fresh data
                            val newInstances =
                                    currentProduct
                                            .instances
                                            .filter { instance -> instance.id != instanceID }
                                            .toMutableList()

                            // 3. Check & Write: Decide what to do based on the modified list
                            if (newInstances.isEmpty()) {
                                // If no instances are left, delete the entire product document
                                transaction.delete(productDocRef)
                            } else {
                                // Otherwise, just update the 'instances' field with the new list
                                transaction.update(productDocRef, "instances", newInstances)
                            }

                            // The transaction will automatically retry if a conflict is detected
                            // (e.g., if someone else added an instance while we were running this)
                        }
                        .await() // Wait for the transaction to complete

                // No need to update 'products.value' manually.
                // The snapshot listener will detect this change and update the UI.

            } catch (e: Exception) {
                // Handle any errors during the transaction
                Log.e("ProductsVM", "Error in deleteInstance transaction", e)
            }
        }
    }

    /**
     * MODIFIED: Uses a transaction to safely add a new product or merge instances. Uses random
     * Firestore document IDs, scanning the in-RAM list for matching name + location.
     */
    fun addOrMergeProduct(productToAdd: Product) {
        val householdId = currentHouseholdId
        if (householdId == null) {
            Log.e("ProductsVM", "Cannot add, householdId is null")
            return
        }

        val productName = productToAdd.name.trim()
        val locationId = productToAdd.location.trim()

        if (productName.isEmpty()) {
            Log.e("ProductsVM", "Cannot add product. Name is empty.")
            return
        }

        // Find an existing product in RAM with the same name (case-insensitive) and same location
        val existingProduct =
                products.value.find {
                    it.name.trim().equals(productName, ignoreCase = true) &&
                            it.location.trim().equals(locationId, ignoreCase = true)
                }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (existingProduct != null && existingProduct.documentId.isNotEmpty()) {
                    // Merge into existing product
                    val productDocRef =
                            db.collection("households")
                                    .document(householdId)
                                    .collection("products")
                                    .document(existingProduct.documentId)

                    db
                            .runTransaction { transaction ->
                                val snapshot = transaction.get(productDocRef)
                                if (snapshot.exists()) {
                                    val currentProduct = snapshot.toObject<Product>()
                                    if (currentProduct != null) {
                                        val allInstances =
                                                (currentProduct.instances + productToAdd.instances)
                                                        .toMutableList()
                                        transaction.update(productDocRef, "instances", allInstances)
                                    }
                                } else {
                                    // Rare case: it was in RAM but deleted from Firestore in the
                                    // meantime. Recreate it.
                                    val newProduct =
                                            productToAdd.copy(
                                                    documentId = existingProduct.documentId
                                            )
                                    transaction.set(productDocRef, newProduct)
                                }
                            }
                            .await()
                } else {
                    // Create new product with auto-generated ID
                    val newDocRef =
                            db.collection("households")
                                    .document(householdId)
                                    .collection("products")
                                    .document() // Auto-generates random ID

                    val newProduct = productToAdd.copy(documentId = newDocRef.id)

                    db
                            .runTransaction { transaction ->
                                transaction.set(newDocRef, newProduct)
                            }
                            .await()
                }
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error in addOrMergeProduct", e)
            }
        }
    }

    /**
     * Moves a single instance from its current product (at the old location) to a new location.
     * Uses RAM scanning to find if the product already exists at the new location.
     */
    fun moveInstance(product: Product, instance: ProductInstance, newLocationId: String) {
        val householdId = currentHouseholdId
        if (householdId == null) {
            Log.e("ProductsVM", "Cannot move, householdId is null")
            return
        }
        if (product.documentId.isEmpty()) {
            Log.e("ProductsVM", "Cannot move, product documentId is missing")
            return
        }

        val productName = product.name.trim()
        val collectionRef = db.collection("households").document(householdId).collection("products")

        val oldDocRef = collectionRef.document(product.documentId)

        // Find if target product exists in the new location
        val targetExistingProduct =
                products.value.find {
                    it.name.trim().equals(productName, ignoreCase = true) &&
                            it.location.trim().equals(newLocationId.trim(), ignoreCase = true) &&
                            it.documentId != product.documentId
                }

        val newDocRef =
                if (targetExistingProduct != null && targetExistingProduct.documentId.isNotEmpty()
                ) {
                    collectionRef.document(targetExistingProduct.documentId)
                } else {
                    collectionRef.document() // Generate new random ID
                }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db
                        .runTransaction { transaction ->
                            // --- Read both docs first (Firestore requires all reads before writes)
                            // ---
                            val oldSnap = transaction.get(oldDocRef)
                            val oldProduct = oldSnap.toObject<Product>() ?: return@runTransaction

                            // Only try to read new doc if we are merging into an existing one
                            var newSnap =
                                    if (targetExistingProduct != null) transaction.get(newDocRef)
                                    else null

                            // 1. Remove the instance from the old product
                            val remainingInstances =
                                    oldProduct.instances.filter { it.id != instance.id }

                            if (remainingInstances.isEmpty()) {
                                transaction.delete(oldDocRef)
                            } else {
                                transaction.update(oldDocRef, "instances", remainingInstances)
                            }

                            // 2. Add the instance (with updated location) to the new product
                            if (newSnap != null && newSnap.exists()) {
                                val existingNew = newSnap.toObject<Product>()
                                if (existingNew != null) {
                                    val merged = (existingNew.instances + instance).toMutableList()
                                    transaction.update(newDocRef, "instances", merged)
                                }
                            } else {
                                // Create a brand-new product doc at the new location
                                val newProduct =
                                        product.copy(
                                                documentId = newDocRef.id,
                                                location = newLocationId,
                                                instances = mutableListOf(instance)
                                        )
                                transaction.set(newDocRef, newProduct)
                            }
                        }
                        .await()

                Log.d(
                        "ProductsVM",
                        "moveInstance: moved '${instance.id}' to location '$newLocationId'"
                )
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error in moveInstance transaction", e)
            }
        }
    }

    // --- Edit Instance Operations ---

    fun updateInstance(product: Product, instanceId: String, newQty: Double, newExpiry: Timestamp) {
        val householdId = currentHouseholdId ?: return
        if (product.documentId.isEmpty()) return

        val productDocRef =
                db.collection("households")
                        .document(householdId)
                        .collection("products")
                        .document(product.documentId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db
                        .runTransaction { transaction ->
                            val snap = transaction.get(productDocRef)
                            val currentProduct = snap.toObject<Product>() ?: return@runTransaction

                            val updatedInstances =
                                    currentProduct
                                            .instances
                                            .map {
                                                if (it.id == instanceId) {
                                                    it.copy(
                                                            quantity = newQty,
                                                            expiryDate = newExpiry
                                                    )
                                                } else {
                                                    it
                                                }
                                            }
                                            .toMutableList()

                            transaction.update(productDocRef, "instances", updatedInstances)
                        }
                        .await()
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error updating instance", e)
            }
        }
    }

    fun renameProductAndUpdateInstance(
            product: Product,
            instanceId: String,
            newName: String,
            newQty: Double,
            newExpiry: Timestamp
    ) {
        val householdId = currentHouseholdId ?: return
        if (product.documentId.isEmpty() || newName.trim().isEmpty()) return

        val productDocRef =
                db.collection("households")
                        .document(householdId)
                        .collection("products")
                        .document(product.documentId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db
                        .runTransaction { transaction ->
                            val snap = transaction.get(productDocRef)
                            val currentProduct = snap.toObject<Product>() ?: return@runTransaction

                            val updatedInstances =
                                    currentProduct
                                            .instances
                                            .map {
                                                if (it.id == instanceId) {
                                                    it.copy(
                                                            quantity = newQty,
                                                            expiryDate = newExpiry
                                                    )
                                                } else {
                                                    it
                                                }
                                            }
                                            .toMutableList()

                            transaction.update(productDocRef, "name", newName.trim())
                            transaction.update(productDocRef, "instances", updatedInstances)
                        }
                        .await()
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error renaming product and updating instance", e)
            }
        }
    }

    fun moveInstanceToNewName(
            product: Product,
            instanceId: String,
            newName: String,
            newQty: Double,
            newExpiry: Timestamp
    ) {
        val householdId = currentHouseholdId ?: return
        if (product.documentId.isEmpty() || newName.trim().isEmpty()) return

        val cleanName = newName.trim()
        val collectionRef = db.collection("households").document(householdId).collection("products")
        val oldDocRef = collectionRef.document(product.documentId)

        // Find if target product with the SAME NEW NAME and SAME LOCATION already exists
        val targetExistingProduct =
                products.value.find {
                    it.name.trim().equals(cleanName, ignoreCase = true) &&
                            it.location.trim().equals(product.location.trim(), ignoreCase = true) &&
                            it.documentId != product.documentId
                }

        val newDocRef =
                if (targetExistingProduct != null && targetExistingProduct.documentId.isNotEmpty()
                ) {
                    collectionRef.document(targetExistingProduct.documentId)
                } else {
                    collectionRef.document() // Auto-generate random ID
                }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                db
                        .runTransaction { transaction ->
                            val oldSnap = transaction.get(oldDocRef)
                            val oldProduct = oldSnap.toObject<Product>() ?: return@runTransaction

                            val newSnap =
                                    if (targetExistingProduct != null) transaction.get(newDocRef)
                                    else null

                            // 1. Find and update the instance, then remove from old product
                            val instanceToMove =
                                    oldProduct
                                            .instances
                                            .find { it.id == instanceId }
                                            ?.copy(quantity = newQty, expiryDate = newExpiry)
                                            ?: return@runTransaction

                            val remainingInstances =
                                    oldProduct.instances.filter { it.id != instanceId }

                            if (remainingInstances.isEmpty()) {
                                transaction.delete(oldDocRef)
                            } else {
                                transaction.update(oldDocRef, "instances", remainingInstances)
                            }

                            // 2. Add modified instance to new product
                            if (newSnap != null && newSnap.exists()) {
                                val existingNew = newSnap.toObject<Product>()
                                if (existingNew != null) {
                                    val merged =
                                            (existingNew.instances + instanceToMove).toMutableList()
                                    transaction.update(newDocRef, "instances", merged)
                                }
                            } else {
                                val newProduct =
                                        product.copy(
                                                documentId = newDocRef.id,
                                                name = cleanName,
                                                instances = mutableListOf(instanceToMove)
                                        )
                                transaction.set(newDocRef, newProduct)
                            }
                        }
                        .await()
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error moving instance to new name", e)
            }
        }
    }

    // filter by custom location strings
    fun getProductsByLocation(selectedLocations: List<String>): Flow<List<Product>> {
        return products.map { list ->
            if (selectedLocations.isEmpty()) {
                list // Optionally return all if no filter is applied, or return empty
            } else {
                list.filter { it.location in selectedLocations }
            }
        }
    }

    var tempName: String = "Produs"
    var tempLocation: String = "Fridge"
    var tempUnit: String = "buc"
    var tempCount: Int = 1
    var tempQuantity: Double = 1.0
    // OFF metadata (set during scan, cleared on reset)
    var tempBrands: String? = null
    var tempGenericName: String? = null
    var tempImageUrl: String? = null
    var tempBarcode: String? = null

    fun resetTempAttributes() {
        tempName = "Produs"
        tempLocation = "Fridge"
        tempUnit = "buc"
        tempCount = 1
        tempQuantity = 1.0
        tempBrands = null
        tempGenericName = null
        tempImageUrl = null
        tempBarcode = null
    }

    fun finalizeAddProduct(instances: MutableList<ProductInstance>) {
        addOrMergeProduct(
                Product.createWithInstances(
                        name = tempName,
                        location = tempLocation,
                        unit = tempUnit,
                        instances = instances,
                        brands = tempBrands,
                        genericName = tempGenericName,
                        imageUrl = tempImageUrl,
                        barcode = tempBarcode
                )
        )
    }

    fun cloneInstance(product: Product, instance: ProductInstance) {
        val householdId = currentHouseholdId ?: return
        if (product.documentId.isEmpty()) return
        val productDocRef =
                db.collection("households")
                        .document(householdId)
                        .collection("products")
                        .document(product.documentId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db
                        .runTransaction { transaction ->
                            val snap = transaction.get(productDocRef)
                            val current = snap.toObject<Product>() ?: return@runTransaction
                            val newId = System.currentTimeMillis().toString()
                            val cloned = instance.copy(id = newId)
                            val updated = (current.instances + cloned).toMutableList()
                            transaction.update(productDocRef, "instances", updated)
                        }
                        .await()
            } catch (e: Exception) {
                Log.e("ProductsVM", "Error cloning instance", e)
            }
        }
    }

    /**
     * Sortează TOATE instanțele după data expirării și le grupează pe produse consecutive. Rezultă
     * o listă care poate avea "Lapte", "Ouă", "Lapte" (din nou).
     */
    fun getProductsSortedByExpiry(selectedLocations: List<String>): Flow<List<Product>> {
        return products.map { productList ->
            // 1. Filtrarea (la fel ca în getProductsByLocation)
            val filteredList =
                    if (selectedLocations.isEmpty()) {
                        productList
                    } else {
                        productList.filter { it.location in selectedLocations }
                    }

            // 2. Creăm o listă "plată" cu *toate* instanțele,
            //    păstrând o referință la produsul părinte.
            //    Folosim un Pair<ProdusOriginal, Instanță>
            val allInstances: List<Pair<Product, ProductInstance>> =
                    filteredList.flatMap { product ->
                        product.instances.map { instance ->
                            // Creăm un Pair (ProdusulPărinte, Instanța)
                            product to instance
                        }
                    }

            // 3. Sortăm lista plată după data de expirare a instanței
            val sortedInstances = allInstances.sortedBy { (_, instance) -> instance.expiryDate }

            // 4. Re-grupăm instanțele sortate.
            //    Creăm o listă nouă de Produse, unde un produs
            //    se poate repeta dacă este întrerupt de altul în sortare.
            val groupedList = mutableListOf<Product>()
            var currentProductGroup: Product? = null

            for ((productInfo, instance) in sortedInstances) {
                // Verificăm dacă instanța curentă este
                // a. Prima din listă (currentProductGroup e null)
                // b. Sau aparține unui produs diferit de grupul curent
                if (currentProductGroup == null ||
                                currentProductGroup.documentId != productInfo.documentId
                ) {

                    // 1. Dacă există un grup anterior, îl adăugăm la listă
                    currentProductGroup?.let { groupedList.add(it) }

                    // 2. Începem un grup nou.
                    //    Creăm o *copie* a produsului, dar cu o listă de instanțe nouă
                    //    care conține DOAR instanța curentă.
                    currentProductGroup = productInfo.copy(instances = mutableListOf(instance))
                } else {
                    // Instanța aparține grupului curent (același produs, consecutiv)
                    // Doar o adăugăm la lista de instanțe a grupului.
                    currentProductGroup.instances.add(instance)
                }
            }

            // Adăugăm ultimul grup format la listă
            currentProductGroup?.let { groupedList.add(it) }

            // 5. Returnăm lista nou creată
            groupedList
        }
    }
}
