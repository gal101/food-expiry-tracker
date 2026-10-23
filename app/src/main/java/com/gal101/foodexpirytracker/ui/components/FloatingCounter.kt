package com.gal101.foodexpirytracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatingCounter(
    count: MutableState<Int>,
    increment: Int,
    shownText: String = "",
    containerColor: Color = MaterialTheme.colorScheme.surface,
    topPadding: Int = 0
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = containerColor,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(top = topPadding.dp)
                .height(56.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                IconButton(onClick = { if(count.value - increment > 0) count.value = count.value - increment }) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrement")
                }
                Text(
                    text = "${count.value}$shownText",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { count.value = count.value + increment }) {
                    Icon(Icons.Default.Add, contentDescription = "Increment")
                }
            }
        }
    }
}
