package com.example.composemodifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composemodifiers.ui.theme.ComposeModifiersTheme

/**
 * HOMEWORK 4: Compose Layout, Modifiers & State
 */

// ===========================================================================
// PART A: The three containers & the layout tree
// ===========================================================================

/**
 * T1 — a Column with arrangement & alignment
 *
 * CONCEPT: Column stacks children vertically. arrangement controls main-axis
 * (vertical) spacing; alignment controls cross-axis (horizontal) positioning.
 */
@Composable
fun Task1(modifier: Modifier = Modifier) {
    // T1: Build a Column that fills width, 16dp padding, 8dp gap, centered.
    Column(
        modifier = modifier
            .fillMaxWidth()                         // take all horizontal space
            .padding(16.dp),                        // outer gutter
        // CHANGED: from Arrangement.spacedBy(8.dp) to Arrangement.spacedBy(24.dp)
        // VISIBLE CHANGE: The items are pushed much further apart vertically,
        // making the list look more "airy" but taking up significantly more height.
        verticalArrangement = Arrangement.spacedBy(24.dp), // // T1: increased gap
        horizontalAlignment = Alignment.CenterHorizontally  // cross-axis centering
    ) {
        Text("Mercury", style = MaterialTheme.typography.titleLarge)
        Text("Closest to the Sun")
        Text("88-day year")
        Button(onClick = {}) { Text("Open") }
    }
}

/**
 * T2 — a Row that shares width with weight
 *
 * CONCEPT: weights split REMAINING space. Elements without weights are measured
 * first; those with weights share what's left.
 */
@Composable
fun Task2(modifier: Modifier = Modifier) {
    // PREDICTION: If weight is removed from Button B, B will shrink to fit its 
    // text ("B (wide)"). Buttons A and C (both weight 1f) will then split all 
    // the remaining width of the Row equally between them.
    Row(
        modifier = modifier
            .fillMaxWidth()                         // take all horizontal space
            .padding(16.dp),                        // outer gutter
        horizontalArrangement = Arrangement.spacedBy(8.dp) // gap between buttons
    ) {
        // T2: 1 : 2 : 1 ratio (original)
        // Update: Experimenting with removing weight from B
        Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("A") }
        
        // Removed .weight(2f) for the experiment
        Button(onClick = {}) { Text("B (wide)") } 
        
        Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("C") }
    }
    // CONFIRMATION: The prediction was correct. Button B is now small, and A and C
    // have grown to fill the rest of the screen.
}

/**
 * T3 — a Box that layers & aligns
 *
 * CONCEPT: Box layers children on top of each other (Z-axis). align() positions
 * children independently relative to the Box edges.
 */
@Composable
fun Task3(modifier: Modifier = Modifier) {
    // T3: 140dp tall Box, tinted bg, centered title, corner badges.
    // COMMENT: Box is right for this because we want multiple elements (badges) 
    // to "float" over the main content (title) in specific corners, which 
    // Row and Column cannot do without complex nested layouts or spacers.
    Box(
        modifier = modifier
            .fillMaxWidth()                         // fill screen width
            .height(140.dp)                         // fixed height
            .background(Color(0xFF22314A)),         // tinted background
        contentAlignment = Alignment.Center         // center children by default
    ) {
        // Main title centered due to contentAlignment above
        Text("Featured planet", color = Color.White)

        // Badge 1 in TopStart (A second badge added per instructions)
        Text(
            text = "NEW", 
            color = Color.Yellow,
            modifier = Modifier
                .align(Alignment.TopStart)          // pin to top-left
                .padding(8.dp)                      // inset from edge
        )

        // Badge 2 moved from TopEnd to BottomEnd
        Text(
            text = "HOT", 
            color = Color.Red,
            modifier = Modifier
                .align(Alignment.BottomEnd)         // pin to bottom-right
                .padding(8.dp)                      // inset from edge
        )
    }
}

// ===========================================================================
// PART B: Modifier order is a feature
// ===========================================================================

/**
 * T4 — padding vs background
 */
@Composable
fun Task4() {
    // PREDICTION: 
    // In A, the green background will include the padding (green fills the whole 
    // area). In B, there will be a transparent gap (green only hugs the text).
    Column(modifier = Modifier.padding(16.dp)) {
        // A — background FIRST: paint fills the area, then padding insets content.
        Text(
            text = "background → padding", 
            modifier = Modifier
                .background(Color(0xFF3DDC84))      // 1. paint green
                .padding(16.dp)                     // 2. inset text
        )
        
        Spacer(Modifier.height(16.dp))

        // B — padding FIRST: inset the area first, then paint only what remains.
        Text(
            text = "padding → background", 
            modifier = Modifier
                .padding(16.dp)                     // 1. reserve outer margin
                .background(Color(0xFF3DDC84))      // 2. paint green inside
        )
    }
    // COMPARISON: The emulator matches the playground exactly. 
    // ONE THING ADDED: The real version in Android Studio adds the Material 
    // typography system, allowing the text to use theme-defined font weights.
}

/**
 * T5 — clip, border & background together
 *
 * CONCEPT: clip() rounds the area for everything that follows (background and content).
 */
@Composable
fun Task5() {
    // T5: Build a rounded "avatar".
    // OBSERVATION: If .background() is moved BEFORE .clip(), the color fills 
    // the original square area, and the clip then cuts the corners of the 
    // *text only*, leaving the background unrounded. To get rounded corners 
    // on the color, we must clip FIRST.
    Box(
        modifier = Modifier
            .size(72.dp)                            // reserve 72x72
            .clip(RoundedCornerShape(16.dp))         // 1. clip everything after
            .background(Color(0xFFB3E5FC))          // 2. fill respects the clip
            .border(
                width = 2.dp, 
                color = Color(0xFF0288D1), 
                shape = RoundedCornerShape(16.dp)   // 3. border follows same shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("JP")
    }
}

/**
 * T6 — size-affecting vs draw-only modifiers
 */
@Composable
fun Task6() {
    // T6: Card with both types.
    // EXPLANATION: Alpha only affects the "drawing" phase; it doesn't change how 
    // much space the card occupies, so siblings don't move. Padding changes 
    // the "measurement" phase, literally shrinking the card's bounds, which 
    // forces sibling elements to shift to fill or respect that new space.
    Card(
        modifier = Modifier
            .fillMaxWidth()                         // // size: take all width
            .padding(16.dp)                         // // size: measured inset
            .shadow(                                // // draw: only visual layer
                elevation = 8.dp, 
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(0.9f)                            // // draw: transparency only
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Olympus Mons", style = MaterialTheme.typography.titleMedium)
            Text("Tallest volcano in the solar system")
        }
    }
}

// ===========================================================================
// PART C: State & interactivity
// ===========================================================================

/**
 * T7 — a counter (state & recomposition)
 */
@Composable
fun Task7() {
    // T7: A counter button.
    // QUESTION: If we use a plain 'var count = 0', nothing happens when we tap.
    // WHY? Because a plain variable doesn't notify Compose that state changed.
    // Even if it increments, Compose won't trigger a "recomposition" to redraw 
    // the UI with the new value.
    var count by remember { mutableStateOf(0) }     // survives recomposition
    
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Count: $count", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { count++ }) {             // update state -> trigger redraw
            Text("Add one") 
        }
    }
}

/**
 * T8 — a real toggle that drives the UI (state hoisting)
 */
@Composable
fun Task8() {                                       // T8: stateful caller
    var on by remember { mutableStateOf(false) }    // owns the state
    
    Column {
        ToggleRow(checked = on, onCheckedChange = { on = it }) // pass state down, event up
        
        // T8: Side effect of the state: show extra content when ON
        if (on) {
            Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Secret details revealed!", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) { // T8: stateless
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = if (checked) "Notifications: ON" else "Notifications: OFF",
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * T9 — a bound TextField
 */
@Composable
fun Task9() {
    // T9: Bound text field.
    // LOOP: User types -> onValueChange calls -> state 'name' updates -> 
    // Compose detects change -> Recomposes Task9 -> OutlinedTextField shows 
    // new value AND the echo Text shows new value simultaneously.
    var name by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = name,                           // controlled by state
            onValueChange = { name = it },          // keystroke -> update state
            label = { Text("Your name") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text("Hello, ${name.ifBlank { "stranger" }} (${name.length} chars)")
    }
}

// ===========================================================================
// PART D: Capstone
// ===========================================================================

/**
 * T10 — capstone: an interactive planet screen
 */
@Composable
fun Task10() {
    // Shared state: which planet IDs are favorited
    var favorites by remember { mutableStateOf(setOf<Int>()) }
    
    val planets = listOf(
        Pair(1, "Mercury"), Pair(2, "Venus"), Pair(3, "Earth"), 
        Pair(4, "Mars"), Pair(5, "Jupiter"), Pair(6, "Saturn")
    )

    Column(modifier = Modifier.fillMaxWidth()) {    // outer Column
        // T3: Banner Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("Planet Explorer", style = MaterialTheme.typography.headlineMedium)
            // T6: Corner badge with draw-only alpha
            Text(
                text = "${favorites.size} favs",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .alpha(0.8f)                    // draw-only
            )
        }

        HorizontalDivider()                         // divider
        
        LazyColumn(modifier = Modifier.fillMaxSize()) { // scrollable list
            items(planets) { (id, name) ->
                val isFav = id in favorites
                // T10: Extracted reusable row
                PlanetRow(
                    name = name,
                    isFavorite = isFav,
                    onToggleFav = {
                        favorites = if (isFav) favorites - id else favorites + id
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PlanetRow(
    name: String, 
    isFavorite: Boolean, 
    onToggleFav: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()                         // size-affecting
            .padding(8.dp)                          // size-affecting
            .shadow(2.dp, RoundedCornerShape(8.dp)) // draw-only
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleFav() }         // interactivity
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(if (isFavorite) "★" else "☆", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// ===========================================================================
// PREVIEWS
// ===========================================================================

@Preview(showBackground = true)
@Composable
fun Homework4Preview() {
    ComposeModifiersTheme {
        Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            Lesson("T1", "Column arrangement") { Task1() }
            Lesson("T2", "Row weight") { Task2() }
            Lesson("T3", "Box layers") { Task3() }
            Lesson("T4", "Padding order") { Task4() }
            Lesson("T5", "Clip vs Background") { Task5() }
            Lesson("T6", "Modifier types") { Task6() }
            Lesson("T7", "Counter state") { Task7() }
            Lesson("T8", "State hoisting toggle") { Task8() }
            Lesson("T9", "Bound TextField") { Task9() }
            Lesson("T10", "Capstone screen") { Task10() }
        }
    }
}
