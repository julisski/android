// =============================================================================
// Type.kt
//
// The app's Material 3 type scale (Typography), exposed as MaterialTheme.typography.
// Only bodyLarge is customized; every other style keeps its Material default.
// =============================================================================
package com.example.websocketlive.ui.theme

import androidx.compose.material3.Typography                // the container for the full text style set
import androidx.compose.ui.text.TextStyle                   // describes one text style (font, size, spacing...)
import androidx.compose.ui.text.font.FontFamily             // which typeface to use (Default = system font)
import androidx.compose.ui.text.font.FontWeight             // weight: Normal, Medium, Bold, ...
import androidx.compose.ui.unit.sp                          // scale-independent pixels (respects user font scaling)

// Set of Material typography styles to start with.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
