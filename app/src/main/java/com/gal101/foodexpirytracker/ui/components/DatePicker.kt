package com.gal101.foodexpirytracker.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.time.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyDatePicker(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var day by remember { mutableStateOf(selectedDate.dayOfMonth) }
    var month by remember { mutableStateOf(selectedDate.monthValue) }
    var year by remember { mutableStateOf(selectedDate.year) }

    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SmoothWheelPicker(
            range = 1..daysInMonth,
            selected = day,
            onValueChange = {
                day = it
                onDateChange(LocalDate.of(year, month, day))
            },
            label = "Zi"
        )
        SmoothWheelPicker(
            range = 1..12,
            selected = month,
            onValueChange = {
                month = it
                day = minOf(day, YearMonth.of(year, month).lengthOfMonth())
                onDateChange(LocalDate.of(year, month, day))
            },
            label = "Lună"
        )
        SmoothWheelPicker(
            range = (LocalDate.now().year - 2)..(LocalDate.now().year + 5),
            selected = year,
            onValueChange = {
                year = it
                day = minOf(day, YearMonth.of(year, month).lengthOfMonth())
                onDateChange(LocalDate.of(year, month, day))
            },
            label = "An"
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SmoothWheelPicker(
    range: IntRange,
    selected: Int,
    onValueChange: (Int) -> Unit,
    label: String
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected - range.first)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val index = listState.firstVisibleItemIndex + 2
            val value = range.first + index.coerceIn(0, range.last - range.first)
            onValueChange(value)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .height(120.dp)
                .width(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF6F6F6))
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 35.dp)
            ) {
                items(range.count()) { index ->
                    val value = range.first + index
                    val isSelected = value == selected
                    Text(
                        text = value.toString(),
                        color = if (isSelected) Color.Black else Color.Gray,
                        fontSize = if (isSelected) 22.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(26.dp)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFAAAAAA), RoundedCornerShape(8.dp))
            )
        }
    }
}
