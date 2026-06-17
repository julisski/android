package com.example.composemodifiers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composemodifiers.ui.theme.ComposeModifiersTheme

/**
 * Exercise: Building a Column with specific arrangement and alignment.
 */
@Composable
fun ColumnExercise(modifier: Modifier = Modifier) {
    // A Column that fills the width, has 16dp of padding,
    // and centers children horizontally.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        // CHANGED: from Arrangement.spacedBy(8.dp) to Arrangement.spacedBy(24.dp).
        // COMMENT: Increasing the spacing from 8.dp to 24.dp visibly pushes the 
        // children much further apart vertically, creating more "white space" 
        // between the Text elements and the Button.
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "First Item")
        Text(text = "Second Item")
        Text(text = "Third Item")
        Button(onClick = { /* Do something */ }) {
            Text("Click Me")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColumnExercisePreview() {
    ComposeModifiersTheme {
        ColumnExercise()
    }
}
