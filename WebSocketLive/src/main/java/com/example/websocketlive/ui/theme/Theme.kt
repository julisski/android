// =============================================================================
// Theme.kt
//
// Defines WebSocketLiveTheme — the single Composable that wraps the whole app UI
// and supplies the Material 3 design system (color scheme + typography). On
// Android 12+ it can use wallpaper-based dynamic color; otherwise it falls back to
// the static light/dark palettes from Color.kt.
// =============================================================================
package com.example.websocketlive.ui.theme

import android.os.Build                                     // to check the device's Android version (SDK_INT)
import androidx.compose.foundation.isSystemInDarkTheme      // reads the system-wide light/dark setting
import androidx.compose.material3.MaterialTheme             // provider that exposes colors/typography to children
import androidx.compose.material3.darkColorScheme           // builder for a dark color scheme
import androidx.compose.material3.dynamicDarkColorScheme    // wallpaper-based dark scheme (Android 12+)
import androidx.compose.material3.dynamicLightColorScheme   // wallpaper-based light scheme (Android 12+)
import androidx.compose.material3.lightColorScheme          // builder for a light color scheme
import androidx.compose.runtime.Composable                  // marks the theme function as a Composable
import androidx.compose.ui.platform.LocalContext            // grabs the current Context (needed for dynamic color)

// The static DARK palette, used when dynamic color is unavailable/disabled.
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// The static LIGHT palette, used when dynamic color is unavailable/disabled.
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * WebSocketLiveTheme — wrap the app's content in this to apply the design system.
 *
 * @param darkTheme    follow the system dark/light setting by default.
 * @param dynamicColor use wallpaper-based color on Android 12+ when available.
 * @param content      the UI to render inside this theme.
 */
@Composable
fun WebSocketLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
