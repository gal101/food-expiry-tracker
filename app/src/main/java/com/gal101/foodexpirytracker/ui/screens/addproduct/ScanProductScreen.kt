package com.gal101.foodexpirytracker.ui.screens.addproduct

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gal101.foodexpirytracker.data.RetrofitClient
import com.gal101.foodexpirytracker.ui.components.BarcodeScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanProductScreen(
        onProductFound:
                (
                        name: String,
                        quantity: Double,
                        unit: String,
                        brands: String?,
                        genericName: String?,
                        imageUrl: String?,
                        barcode: String?) -> Unit,
        onManualEntry: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var isScanning by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf(false) } // Track error state

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isScanning && !scanError) {
                BarcodeScanner(
                        onBarcodeScanned = { barcode ->
                            if (!isLoading && !scanError) {
                                isLoading = true
                                isScanning = false // Stop further scans momentarily
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.api.getProduct(barcode)
                                        val product = response.product

                                        // Check if product exists and status is found (1)
                                        if (response.status == 1 && product != null) {
                                            // DEBUG: Show what we got
                                            val debugMsg =
                                                    "Name: ${product.productName} | Brands: ${product.brands} | Gen: ${product.genericName}"

                                            // --- DEBUG TOAST (Enabled per user request) ---
                                            //
                                            // scope.launch {
                                            //
                                            // Toast.makeText(
                                            //
                                            //          context,
                                            //
                                            //          "API SUCCESS: $debugMsg",
                                            //
                                            //          Toast.LENGTH_LONG
                                            //
                                            //  )
                                            //
                                            //  .show()
                                            //                                            }
                                            // --------------------------------------------------

                                            // Prioritize Romanian name, then English, then generic,
                                            // then Brands, then Generic Name
                                            val rawName =
                                                    product.productNameRo
                                                            ?: product.productNameEn
                                                                    ?: product.productName
                                                                    ?: product.brands
                                                                    ?: product.genericNameRo
                                                                    ?: product.genericName

                                            if (!rawName.isNullOrBlank()) {
                                                val name = rawName

                                                // Try to parse quantity
                                                var quantityVal = product.productQuantity ?: 1.0
                                                var unit = product.productQuantityUnit ?: "Unit"

                                                // Logic: if ml and >= 500, convert to L
                                                if (unit.equals("ml", ignoreCase = true) &&
                                                                quantityVal >= 500
                                                ) {
                                                    quantityVal /= 1000
                                                    unit = "L"
                                                }

                                                // Normalize unit if needed
                                                val normalizedUnit =
                                                        when (unit.lowercase()) {
                                                            "l", "liter", "liters" -> "L"
                                                            "ml" -> "ml"
                                                            "kg", "kilogram" -> "kg"
                                                            "g", "gram" -> "g"
                                                            else -> "Unit"
                                                        }

                                                // No artificial delay here
                                                onProductFound(
                                                        name,
                                                        quantityVal,
                                                        normalizedUnit,
                                                        product.brands,
                                                        product.genericName
                                                                ?: product.genericNameRo,
                                                        product.imageUrl,
                                                        barcode
                                                )
                                            } else {
                                                // Product found but NO NAME -> Treat as Error
                                                scanError = true
                                                // Debug Toast to see why
                                                //
                                                // scope.launch {
                                                //
                                                //  Toast.makeText(
                                                //
                                                //                  context,
                                                //
                                                //                  "No Name found! API Data:
                                                // $debugMsg",
                                                //
                                                //                  Toast.LENGTH_LONG
                                                //
                                                //          )
                                                //
                                                //          .show()
                                                //                                                }
                                            }
                                        } else {
                                            // Product NOT found or Status != 1
                                            scanError = true
                                            // Trigger feedback
                                        }
                                    } catch (e: Exception) {
                                        // Network or parsing error
                                        scanError = true
                                        //                                        Toast.makeText(
                                        //
                                        // context,
                                        //
                                        // "Error: ${e.message}",
                                        //
                                        // Toast.LENGTH_SHORT
                                        //                                                )
                                        //                                                .show()
                                    } finally {
                                        isLoading = false
                                        if (scanError) {
                                            // If error, wait 2 seconds then resume scanning
                                            kotlinx.coroutines.delay(2000)
                                            scanError = false
                                            isScanning = true
                                        }
                                    }
                                }
                            }
                        }
                )
            }

            // --- Error Overlay (Red Flash) ---
            if (scanError) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        border =
                                androidx.compose.foundation.BorderStroke(
                                        4.dp,
                                        MaterialTheme.colorScheme.error
                                )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Default.Warning, // Use Warning icon
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                    text = "Product Not Found!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onError
                            )
                            Text(
                                    text = "Try again or enter manually.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }

            // Overlay for Manual Entry (Always visible at bottom)
            Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 50.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = onManualEntry) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Enter Manually")
                }
            }

            if (isLoading) {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Fetching product details...")
                    }
                }
            }
        }
    } else {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required to scan barcodes.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Request Permission")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onManualEntry) { Text("Enter Manually") }
        }
    }
}
