package com.example.grocery.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Google Keep-like Light Colors
private val KeepWhite = Color(0xFFFFFFFF)
private val KeepBackground = Color(0xFFFFFFFF) // Main background
private val KeepSurface = Color(0xFFFFFFFF)
private val KeepOnSurface = Color(0xFF202124) // Text color
private val KeepSecondary = Color(0xFF5F6368) // Secondary text/icons

// Google Keep-like Dark Colors (Approximate)
private val KeepDarkBackground = Color(0xFF202124)
private val KeepDarkSurface = Color(0xFF525355) // Card/Surface color
private val KeepDarkOnSurface = Color(0xFFE8EAED)

private val DarkColorScheme = darkColorScheme(
    primary = KeepDarkOnSurface,
    background = KeepDarkBackground,
    surface = KeepDarkBackground,
    onPrimary = KeepDarkBackground,
    onBackground = KeepDarkOnSurface,
    onSurface = KeepDarkOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = KeepOnSurface,
    background = KeepBackground,
    surface = KeepSurface,
    onPrimary = KeepWhite,
    onBackground = KeepOnSurface,
    onSurface = KeepOnSurface,
    primaryContainer = KeepBackground,
    onPrimaryContainer = KeepOnSurface
)

@Composable
fun GroceryListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce our Keep-like aesthetic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
