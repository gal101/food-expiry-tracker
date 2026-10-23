package com.gal101.foodexpirytracker.ui.screens.addproduct

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anhaki.picktime.PickDate
import com.anhaki.picktime.utils.PickTimeFocusIndicator
import com.anhaki.picktime.utils.PickTimeTextStyle
import com.gal101.foodexpirytracker.data.model.CustomLocation
import com.gal101.foodexpirytracker.ui.components.FloatingCounter
import com.gal101.foodexpirytracker.ui.components.ProductInstance
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstancesScreen(
        count: Int,
        unit: String,
        location: String,
        locations: List<CustomLocation>,
        productName: String,
        initialQuantity: Double = 1.0,
        onBack: () -> Unit = {},
        onFinish: (MutableList<ProductInstance>, String) -> Unit
) {

        // State for the number of items, initialized with the passed count
        val currentCount = remember { mutableIntStateOf(count) }

        val context = LocalContext.current
        val sharedPrefs = remember {
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        }

        val initialLocationId =
                remember(locations) {
                        if (locations.any { it.id == location }) {
                                location
                        } else {
                                val savedId = sharedPrefs.getString("last_location_id", null)
                                if (savedId != null && locations.any { it.id == savedId }) {
                                        savedId
                                } else {
                                        locations.firstOrNull()?.id ?: ""
                                }
                        }
                }

        var selectedLocationId by remember(initialLocationId) { mutableStateOf(initialLocationId) }
        var locationDropdownExpanded by remember { mutableStateOf(false) }

        val currentDate = LocalDate.now()
        val preferredDays = remember {
                mutableStateListOf<Int>().apply { repeat(count) { add(currentDate.dayOfMonth) } }
        }
        val days = remember {
                mutableStateListOf<Int>().apply { repeat(count) { add(currentDate.dayOfMonth) } }
        }
        val months = remember {
                mutableStateListOf<Int>().apply { repeat(count) { add(currentDate.monthValue) } }
        }
        val years = remember {
                mutableStateListOf<Int>().apply { repeat(count) { add(currentDate.year) } }
        }
        val quantitiesDouble = remember {
                mutableStateListOf<MutableState<Double>>().apply {
                        repeat(count) { add(mutableDoubleStateOf(initialQuantity)) }
                }
        }
        val quantitiesInt = remember {
                mutableStateListOf<MutableState<Int>>().apply {
                        repeat(count) { add(mutableIntStateOf(initialQuantity.toInt())) }
                }
        }

        // Sync lists with currentCount
        LaunchedEffect(currentCount.intValue) {
                val diff = currentCount.intValue - years.size
                if (diff > 0) {
                        repeat(diff) {
                                preferredDays.add(currentDate.dayOfMonth)
                                days.add(currentDate.dayOfMonth)
                                months.add(currentDate.monthValue)
                                years.add(currentDate.year)
                                quantitiesDouble.add(mutableDoubleStateOf(initialQuantity))
                                quantitiesInt.add(mutableIntStateOf(initialQuantity.toInt()))
                        }
                } else if (diff < 0) {
                        repeat(-diff) {
                                if (years.isNotEmpty()) {
                                        val lastIdx = years.lastIndex
                                        preferredDays.removeAt(lastIdx)
                                        days.removeAt(lastIdx)
                                        months.removeAt(lastIdx)
                                        years.removeAt(lastIdx)
                                        quantitiesDouble.removeAt(lastIdx)
                                        quantitiesInt.removeAt(lastIdx)
                                }
                        }
                }
        }

        Scaffold(
                floatingActionButtonPosition = FabPosition.Center,
                topBar = {
                        CenterAlignedTopAppBar(
                                title = { Text("Edit expiring dates") },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                actions = {
                                        TextButton(
                                                onClick = {
                                                        onFinish(
                                                                (0 until currentCount.intValue)
                                                                        .map { i ->
                                                                                val expiryLocalDate:
                                                                                        LocalDate =
                                                                                        LocalDate
                                                                                                .of(
                                                                                                        years[
                                                                                                                i],
                                                                                                        months[
                                                                                                                i],
                                                                                                        days[
                                                                                                                i]
                                                                                                )
                                                                                val expiryJavaDate:
                                                                                        Date =
                                                                                        Date.from(
                                                                                                expiryLocalDate
                                                                                                        .atStartOfDay(
                                                                                                                ZoneId.systemDefault()
                                                                                                        )
                                                                                                        .toInstant()
                                                                                        )
                                                                                val expiryTimestamp:
                                                                                        Timestamp =
                                                                                        Timestamp(
                                                                                                expiryJavaDate
                                                                                        )
                                                                                ProductInstance(
                                                                                        quantity =
                                                                                                if (unit ==
                                                                                                                "Unit"
                                                                                                )
                                                                                                        quantitiesInt[
                                                                                                                        i]
                                                                                                                .value
                                                                                                                .toDouble()
                                                                                                else
                                                                                                        quantitiesDouble[
                                                                                                                        i]
                                                                                                                .value,
                                                                                        expiryDate =
                                                                                                expiryTimestamp
                                                                                )
                                                                        }
                                                                        .toMutableList(),
                                                                selectedLocationId
                                                        )
                                                        sharedPrefs
                                                                .edit()
                                                                .putString(
                                                                        "last_location_id",
                                                                        selectedLocationId
                                                                )
                                                                .apply()
                                                },
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                        ) {
                                                Text(
                                                        "Finish",
                                                        style = MaterialTheme.typography.titleMedium
                                                )
                                        }
                                },
                        )
                },
                floatingActionButton = {
                        FloatingCounter(count = currentCount, increment = 1, shownText = " items")
                }
        ) { padding ->
                LazyColumn(
                        modifier =
                                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                        contentPadding =
                                PaddingValues(
                                        top = 16.dp,
                                        bottom = 100.dp
                                ), // Extra bottom padding for FAB
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        item {
                                Card(
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant
                                                ),
                                        modifier =
                                                Modifier.fillMaxWidth(0.9f).padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(12.dp)
                                ) {
                                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                                Text(
                                                        "Select Location",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .labelMedium,
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box {
                                                        OutlinedCard(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .clickable {
                                                                                        locationDropdownExpanded =
                                                                                                true
                                                                                },
                                                                colors =
                                                                        CardDefaults
                                                                                .outlinedCardColors(
                                                                                        containerColor =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surface
                                                                                )
                                                        ) {
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                16.dp
                                                                                        ),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        val selectedLoc =
                                                                                locations.find {
                                                                                        it.id ==
                                                                                                selectedLocationId
                                                                                }
                                                                        val selectedLocationName =
                                                                                selectedLoc?.name
                                                                                        ?: "No Location"
                                                                        val selectedLocationIcon =
                                                                                if (selectedLoc !=
                                                                                                null
                                                                                ) {
                                                                                        com.gal101
                                                                                                .foodexpirytracker
                                                                                                .utils
                                                                                                .LocationIconMapper
                                                                                                .getIconFromName(
                                                                                                        selectedLoc
                                                                                                                .iconName
                                                                                                )
                                                                                } else {
                                                                                        androidx.compose
                                                                                                .material
                                                                                                .icons
                                                                                                .Icons
                                                                                                .Default
                                                                                                .Close
                                                                                }

                                                                        Row(
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                selectedLocationIcon,
                                                                                        contentDescription =
                                                                                                selectedLocationName,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                )
                                                                                )
                                                                                Spacer(
                                                                                        modifier =
                                                                                                Modifier.width(
                                                                                                        8.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        selectedLocationName,
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge
                                                                                )
                                                                        }
                                                                        Icon(
                                                                                Icons.Default
                                                                                        .ArrowDropDown,
                                                                                contentDescription =
                                                                                        "Select Location"
                                                                        )
                                                                }
                                                        }
                                                        DropdownMenu(
                                                                expanded = locationDropdownExpanded,
                                                                onDismissRequest = {
                                                                        locationDropdownExpanded =
                                                                                false
                                                                }
                                                        ) {
                                                                locations.forEach { loc ->
                                                                        val icon =
                                                                                com.gal101
                                                                                        .foodexpirytracker
                                                                                        .utils
                                                                                        .LocationIconMapper
                                                                                        .getIconFromName(
                                                                                                loc.iconName
                                                                                        )
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically
                                                                                        ) {
                                                                                                Icon(
                                                                                                        imageVector =
                                                                                                                icon,
                                                                                                        contentDescription =
                                                                                                                loc.name,
                                                                                                        modifier =
                                                                                                                Modifier.size(
                                                                                                                        24.dp
                                                                                                                )
                                                                                                )
                                                                                                Spacer(
                                                                                                        modifier =
                                                                                                                Modifier.width(
                                                                                                                        8.dp
                                                                                                                )
                                                                                                )
                                                                                                Text(
                                                                                                        loc.name
                                                                                                )
                                                                                        }
                                                                                },
                                                                                onClick = {
                                                                                        selectedLocationId =
                                                                                                loc.id
                                                                                        locationDropdownExpanded =
                                                                                                false
                                                                                },
                                                                                trailingIcon = {
                                                                                        if (selectedLocationId ==
                                                                                                        loc.id
                                                                                        ) {
                                                                                                Icon(
                                                                                                        Icons.Default
                                                                                                                .Check,
                                                                                                        contentDescription =
                                                                                                                "Selected",
                                                                                                        tint =
                                                                                                                MaterialTheme
                                                                                                                        .colorScheme
                                                                                                                        .primary
                                                                                                )
                                                                                        }
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        items(years.size) { index ->
                                val no = index + 1
                                Card(
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.primary
                                                ),
                                        modifier = Modifier.wrapContentHeight().fillMaxWidth(0.9f),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                                // Title Section
                                                Text(
                                                        text = "$productName $no",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Expiry Date Section
                                                if (!location.equals("Non-food", ignoreCase = true)
                                                ) {
                                                        Text(
                                                                text = "Expiring date",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleMedium,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimary
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Card(
                                                                shape = RoundedCornerShape(12.dp),
                                                                colors =
                                                                        CardDefaults.cardColors(
                                                                                containerColor =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .surface
                                                                        ),
                                                                elevation =
                                                                        CardDefaults.cardElevation(
                                                                                1.dp
                                                                        ),
                                                                modifier =
                                                                        Modifier.fillMaxWidth(0.95f)
                                                        ) {
                                                                PickDate(
                                                                        initialDay = days[index],
                                                                        onDayChange = { newDay ->
                                                                                val maxDay =
                                                                                        YearMonth
                                                                                                .of(
                                                                                                        years[
                                                                                                                index],
                                                                                                        months[
                                                                                                                index]
                                                                                                )
                                                                                                .lengthOfMonth()
                                                                                preferredDays[
                                                                                        index] =
                                                                                        newDay
                                                                                days[index] =
                                                                                        newDay.coerceIn(
                                                                                                1,
                                                                                                maxDay
                                                                                        )
                                                                        },
                                                                        initialMonth =
                                                                                months[index],
                                                                        onMonthChange = { newMonth
                                                                                ->
                                                                                months[index] =
                                                                                        newMonth
                                                                                val maxDay =
                                                                                        YearMonth
                                                                                                .of(
                                                                                                        years[
                                                                                                                index],
                                                                                                        newMonth
                                                                                                )
                                                                                                .lengthOfMonth()
                                                                                days[index] =
                                                                                        preferredDays[
                                                                                                        index]
                                                                                                .coerceIn(
                                                                                                        1,
                                                                                                        maxDay
                                                                                                )
                                                                        },
                                                                        initialYear = years[index],
                                                                        onYearChange = { newYear ->
                                                                                years[index] =
                                                                                        newYear
                                                                                val maxDay =
                                                                                        YearMonth
                                                                                                .of(
                                                                                                        newYear,
                                                                                                        months[
                                                                                                                index]
                                                                                                )
                                                                                                .lengthOfMonth()
                                                                                days[index] =
                                                                                        preferredDays[
                                                                                                        index]
                                                                                                .coerceIn(
                                                                                                        1,
                                                                                                        maxDay
                                                                                                )
                                                                        },
                                                                        selectedTextStyle =
                                                                                PickTimeTextStyle(
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary,
                                                                                        fontSize =
                                                                                                18.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .SemiBold,
                                                                                ),
                                                                        unselectedTextStyle =
                                                                                PickTimeTextStyle(
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant,
                                                                                        fontSize =
                                                                                                14.sp,
                                                                                ),
                                                                        verticalSpace = 4.dp,
                                                                        horizontalSpace = 8.dp,
                                                                        containerColor =
                                                                                Color.Transparent,
                                                                        isLooping = true,
                                                                        extraRow = 1,
                                                                        focusIndicator =
                                                                                PickTimeFocusIndicator(
                                                                                        enabled =
                                                                                                false
                                                                                )
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                }

                                                // Quantity Display Section (Read-Only)
                                                ReadOnlyQuantityDisplay(
                                                        quantity =
                                                                if (unit == "Unit")
                                                                        quantitiesInt[index].value
                                                                                .toDouble()
                                                                else quantitiesDouble[index].value,
                                                        unit = unit
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun ReadOnlyQuantityDisplay(
        quantity: Double,
        unit: String,
        containerColor: Color = MaterialTheme.colorScheme.surface
) {
        Surface(
                shape = RoundedCornerShape(50),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = containerColor,
                modifier =
                        Modifier.height(40.dp) // Smaller height
                                .wrapContentHeight()
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                        val quantityText =
                                if (unit == "Unit") {
                                        "${quantity.toInt()} $unit"
                                } else {
                                        "${String.format("%.1f", quantity)} $unit"
                                }

                        Text(
                                text = quantityText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                }
        }
}
