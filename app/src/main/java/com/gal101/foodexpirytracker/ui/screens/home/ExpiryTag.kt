package com.gal101.foodexpirytracker.ui.screens.home

import androidx.compose.ui.graphics.Color
import com.gal101.foodexpirytracker.ui.components.Product
import com.gal101.foodexpirytracker.ui.components.ProductInstance
import com.gal101.foodexpirytracker.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ExpiryTag(val text: String, val background: Color, val textColor: Color)

fun getExpiryTag(
        product: Product,
        instance: ProductInstance,
        warningThresholdDays: Int = 7
): ExpiryTag? {
        if (product.location.equals("Non-food", ignoreCase = true)) return null

        val expiryLocalDate =
                instance.expiryDate
                        .toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, expiryLocalDate)

        return when {
                daysRemaining < 0 ->
                        ExpiryTag(
                                text = "Expired",
                                background = ExpiredColor,
                                textColor = ExpiredTextColor
                        )
                daysRemaining == 0L ->
                        ExpiryTag(
                                text = "Today!",
                                background = ExpiresTodayColor,
                                textColor = ExpiresTodayTextColor
                        )
                daysRemaining < warningThresholdDays ->
                        ExpiryTag(
                                text = "$daysRemaining days",
                                background = ExpiresSoonColor,
                                textColor = ExpiresSoonTextColor
                        )
                daysRemaining <= 14 ->
                        ExpiryTag(
                                text = "$daysRemaining days",
                                background = ExpiresOkColor,
                                textColor = ExpiresOkTextColor
                        )
                else -> {
                        val dateText =
                                expiryLocalDate.format(
                                        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
                                )
                        ExpiryTag(
                                text = dateText,
                                background = ExpiresOkColor,
                                textColor = ExpiresOkTextColor
                        )
                }
        }
}
