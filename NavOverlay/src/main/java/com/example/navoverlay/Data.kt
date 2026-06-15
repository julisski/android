package com.example.navoverlay

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

// ===========================================================================
// DATA
// A tiny in-memory data source (same planet domain as the sibling samples). In a
// real app this would come from Room / Retrofit / a repository; hardcoded lists
// are enough to demonstrate a list -> detail flow plus a dialog OVERLAY.
// ===========================================================================

/**
 * A single planet shown on the LIST screen and the DETAIL screen, and whose
 * [fact] is what the dialog OVERLAY pops up on top of the detail screen.
 * @param id    — stable unique identifier; this is what travels in the nav key.
 * @param title — short name shown as the row/headline.
 * @param blurb — one-line description shown under the title.
 * @param fact  — a longer "fun fact" shown INSIDE the overlay dialog.
 */
data class Item(val id: Int, val title: String, val blurb: String, val fact: String)

// The planets rendered on the list screen.
val sampleItems = listOf(
    Item(1, "Mercury", "The smallest planet and the closest to the Sun.",
        "A year on Mercury is just 88 Earth days, but a single day lasts 176."),
    Item(2, "Venus", "The hottest planet, wrapped in thick clouds of acid.",
        "Venus spins backwards, so the Sun rises in the west and sets in the east."),
    Item(3, "Earth", "The only planet known to support life — so far.",
        "Earth is the only planet not named after a Greek or Roman god."),
    Item(4, "Mars", "The red planet, a frequent target for rovers.",
        "Mars hosts Olympus Mons, the tallest volcano in the solar system."),
    Item(5, "Jupiter", "The largest planet, a gas giant with a great red spot.",
        "Jupiter's Great Red Spot is a storm wider than the entire Earth."),
    Item(6, "Saturn", "The ringed gas giant, second largest in the system.",
        "Saturn is so light it would float in water — if you found a big enough tub."),
)

// --- Lookups ----------------------------------------------------------------
// The screens receive only an id (inside their nav key) and resolve the full
// object here. Passing just the id — rather than the whole object — keeps each
// navigation key tiny and serializable.

// Resolve a single Item by its id (used by the detail screen and the overlay).
fun itemById(id: Int): Item = sampleItems.first { it.id == id }

// ===========================================================================
// PREVIEW PROVIDERS — feed each planet into a @Preview, one render per item.
// ===========================================================================

class ItemPreviewProvider : PreviewParameterProvider<Item> {
    override val values: Sequence<Item> = sampleItems.asSequence()
}
