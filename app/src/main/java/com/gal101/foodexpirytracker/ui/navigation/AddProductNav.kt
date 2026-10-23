package com.gal101.foodexpirytracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gal101.foodexpirytracker.ui.components.ProductsViewModel
import com.gal101.foodexpirytracker.ui.screens.addproduct.AddDetailsScreen
import com.gal101.foodexpirytracker.ui.screens.addproduct.AddInstancesScreen
import com.gal101.foodexpirytracker.ui.screens.addproduct.ScanProductScreen
import com.gal101.foodexpirytracker.ui.screens.manage_locations.LocationsViewModel

// import com.gal101.foodexpirytracker.ui.screens.addproduct.*

@Composable
fun AddProductNav(
        parentNavController: NavController,
        viewModel: ProductsViewModel,
        locationsViewModel: LocationsViewModel
) {
    val navController = rememberNavController()

    val locations by locationsViewModel.locations.collectAsState()

    NavHost(navController = navController, startDestination = "scan") {
        composable("scan") {
            ScanProductScreen(
                    onProductFound = { name, quantity, unit, brands, genericName, imageUrl, barcode
                        ->
                        viewModel.tempName = name
                        viewModel.tempQuantity = quantity
                        viewModel.tempCount = 1
                        viewModel.tempUnit = unit
                        viewModel.tempLocation = "Fridge"
                        viewModel.tempBrands = brands
                        viewModel.tempGenericName = genericName
                        viewModel.tempImageUrl = imageUrl
                        viewModel.tempBarcode = barcode
                        navController.navigate("instances")
                    },
                    onManualEntry = { navController.navigate("details") }
            )
        }

        composable("details") {
            AddDetailsScreen(
                    unit = viewModel.tempUnit,
                    name = viewModel.tempName,
                    count = viewModel.tempCount,
                    onNext = { name, unit, quantity ->
                        viewModel.tempName = name
                        viewModel.tempUnit = unit
                        viewModel.tempQuantity = quantity
                        viewModel.tempCount = 1
                        navController.navigate("instances")
                    },
                    onBack = { navController.popBackStack() }
            )
        }

        composable("instances") {
            AddInstancesScreen(
                    count = viewModel.tempCount,
                    unit = viewModel.tempUnit,
                    location = viewModel.tempLocation,
                    locations = locations,
                    onBack = { navController.popBackStack() },
                    onFinish = { instances, selectedLocationId ->
                        viewModel.tempLocation = selectedLocationId
                        viewModel.finalizeAddProduct(instances)
                        parentNavController.popBackStack("my_products", false)
                    },
                    productName = viewModel.tempName,
                    initialQuantity = viewModel.tempQuantity
            )
        }
    }
}
