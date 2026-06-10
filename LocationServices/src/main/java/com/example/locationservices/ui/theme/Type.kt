// =============================================================================
// Type.kt
//
// The app's Material 3 type scale (Typography), exposed as MaterialTheme.typography.
// Material 3 ships sensible defaults for every text style; this file only customizes
// bodyLarge and leaves the rest at their defaults.
// =============================================================================
package com.example.locationservices.ui.theme

import androidx.compose.material3.Typography                // the container for the full text style set
import androidx.compose.ui.text.TextStyle                   // describes one text style (font, size, spacing...)
import androidx.compose.ui.text.font.FontFamily             // which typeface to use (Default = system font)
import androidx.compose.ui.text.font.FontWeight             // weight: Normal, Medium, Bold, ...
import androidx.compose.ui.unit.sp                          // scale-independent pixels (respects user font scaling)

// Set of Material typography styles to start with.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,                   // use the platform's default typeface
        fontWeight = FontWeight.Normal,                    // regular (non-bold) weight
        fontSize = 16.sp,                                  // text size
        lineHeight = 24.sp,                                // vertical space per line
        letterSpacing = 0.5.sp                             // slight extra space between characters
    )
)
