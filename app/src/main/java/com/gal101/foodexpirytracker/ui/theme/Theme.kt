package com.gal101.foodexpirytracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
        darkColorScheme(
                primary = Blue80, // Primary – soft blue
                onPrimary = DarkBlue10, // Text on primary
                primaryContainer = DarkBlue30, // Deep navy – used by bottom bar etc.
                onPrimaryContainer = LightBlue90, // Text on containers – very light blue
                secondary = BlueGrey80,
                tertiary = LightBlue80,
                background = DarkBackground, // Near-black with blue tint (0xFF101014)
                surface = DarkBackground,
                surfaceVariant = Color(0xFF1E1E24), // Slightly lighter than background for cards
                onBackground = Color(0xFFE3E2E6),
                onSurface = Color(0xFFE3E2E6),
                onSurfaceVariant = Color(0xFFBFC6DC)
        )

private val LightColorScheme =
        lightColorScheme(
                primary = Blue40, // Culoarea principală (ex: FAB-uri) - Albastru puternic
                onPrimary = Color.White, // Text/icon pe Primary - Alb
                primaryContainer =
                        LightBlue90, // Culoarea containerelor (ex: Carduri) - Albastru foarte
                // deschis
                onPrimaryContainer = DarkBlue10, // Text/icon pe Containere - Albastru foarte închis
                secondary = BlueGrey40, // Culoarea secundară
                tertiary = LightBlue40, // A treia culoare
                background = LightBackground, // Fundalul principal - Alb
                surface = LightBackground, // Suprafața principală - Alb
                onBackground = DarkBackground, // Textul pe fundal - Închis
                onSurface = DarkBackground // Textul pe suprafețe - Închis
        )

object AppColors {
    val FridgeCardBackground = Color(0xFFE9FBFF)
    val FridgeForeground = Color(0xFF1A2D45)
    val PantryCardBackground = Color(0xFFCCA463)
}

@Composable
fun FoodExpiryTrackerTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false,
        content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
