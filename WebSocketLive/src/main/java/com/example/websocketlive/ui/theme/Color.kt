// =============================================================================
// Color.kt
//
// The raw color palette for the app's Material 3 theme. Theme.kt maps these onto
// Material "roles" for light and dark. "80" tones are light/pastel (DARK theme);
// "40" tones are deeper/saturated (LIGHT theme).
// =============================================================================
package com.example.websocketlive.ui.theme

import androidx.compose.ui.graphics.Color                  // Compose's color type

// --- Dark-theme accents (light/pastel tones) ---------------------------------
val Purple80 = Color(0xFFD0BCFF)                            // primary accent in dark theme
val PurpleGrey80 = Color(0xFFCCC2DC)                        // secondary accent in dark theme
val Pink80 = Color(0xFFEFB8C8)                              // tertiary accent in dark theme

// --- Light-theme accents (deeper/saturated tones) ----------------------------
val Purple40 = Color(0xFF6650a4)                            // primary accent in light theme
val PurpleGrey40 = Color(0xFF625b71)                        // secondary accent in light theme
val Pink40 = Color(0xFF7D5260)                              // tertiary accent in light theme
