package com.example.pawalert.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Amber40,
    secondary = Amber80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = DarkBackground,
    onBackground = Color(0xFFF5EFEB),
    onSurface = Color(0xFFF5EFEB),
    onSurfaceVariant = Color(0xFFD7CCC8)
)

private val LightColors = lightColorScheme(
    primary = Amber40,
    secondary = Brown40,
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF7EFE8),
    onPrimary = Color.White,
    onBackground = Color(0xFF2C2723),
    onSurface = Color(0xFF2C2723)
)

@Composable
fun PawAlertTheme(
    darkTheme: Boolean = true, // Defaults to the rich dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = view.context
        (context as? Activity)?.window?.let { window ->
            try {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            } catch (_: Throwable) {}
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
