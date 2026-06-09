// =============================================================================
// MainActivity.kt
//
// CONCEPT: JETPACK COMPOSE FUNDAMENTALS (single screen, "notes" domain).
//
// This file is a TEACHING ARTIFACT. It deliberately over-comments the ideas a
// beginner should learn first when building UI with Jetpack Compose + Material 3:
//
//   1. COMPOSABLES          — functions marked @Composable that DESCRIBE UI.
//                             You don't mutate widgets; you re-run the function
//                             and Compose figures out what changed ("recompose").
//   2. THE MATERIAL 3 THEME — colors come from MaterialTheme.colorScheme and
//                             text styles from MaterialTheme.typography, so the
//                             whole app stays visually consistent.
//   3. LAYOUT               — Column (vertical), Row (horizontal), Box (overlap),
//                             plus spacing (Spacer/padding) and alignment.
//   4. STATE HOISTING       — the headline lesson. A "stateless" child takes its
//                             value as a PARAMETER and reports changes via a
//                             CALLBACK; the PARENT owns the actual state. This is
//                             "state flows DOWN, events flow UP".
//   5. REUSABLE COMPONENTS  — small, single-purpose @Composables (NoteCard,
//                             SectionHeader, PrimaryButton, FavoriteToggle) that
//                             can be composed together and previewed in isolation.
//   6. @Preview             — render UI in Android Studio's design pane WITHOUT
//                             an emulator. Previews can only run STATELESS UI with
//                             hand-supplied data, which is exactly why hoisting
//                             matters.
//
// WHAT TO INSPECT (student checklist):
//   • Find every place a value is passed DOWN as a parameter and every callback
//     passed for events flowing UP — that pair IS state hoisting.
//   • Notice NotesScreen has TWO overloads: a STATELESS one (pure parameters) and
//     a STATEFUL one (owns rememberSaveable state and calls the stateless one).
//   • See how @Preview renders the STATELESS overload with fake data and never
//     constructs the stateful version.
//   • Compare the STATEFUL-vs-STATELESS comment block above NotesScreen.
//
// Reading order below: package + imports, then DATA, then the reusable UI
// COMPONENTS, then the STATE-owning screen, then @Preview functions.
// =============================================================================

// The package declaration. Every declaration in this file lives in this
// namespace, which mirrors the folder path under src/main/java/.
package com.example.composemodernui

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                     // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                   // base Activity class that can host a Compose UI
import androidx.activity.compose.setContent                  // installs a Compose UI tree as the Activity's content
import androidx.activity.enableEdgeToEdge                    // lets the app draw behind the system status/nav bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.layout.Arrangement        // controls spacing BETWEEN children in a Row/Column
import androidx.compose.foundation.layout.Box                // a layout that stacks/overlaps children on the z-axis
import androidx.compose.foundation.layout.Column             // a layout that stacks children VERTICALLY
import androidx.compose.foundation.layout.Row                // a layout that places children HORIZONTALLY
import androidx.compose.foundation.layout.Spacer             // an empty element used to insert a fixed-size gap
import androidx.compose.foundation.layout.fillMaxSize        // modifier: take ALL available width AND height
import androidx.compose.foundation.layout.fillMaxWidth       // modifier: take ALL available width
import androidx.compose.foundation.layout.height             // modifier: force a specific height (used on Spacers)
import androidx.compose.foundation.layout.padding            // modifier: add empty space AROUND content
import androidx.compose.foundation.lazy.LazyColumn           // a scrolling list that only composes visible rows
import androidx.compose.foundation.lazy.items                // emits one item per element of a List in a LazyColumn

// --- Material 3 component imports ---------------------------------------------
import androidx.compose.material3.Button                     // a filled, tappable Material button
import androidx.compose.material3.Card                       // a surface with rounded corners + elevation (a "card")
import androidx.compose.material3.MaterialTheme              // entry point to the theme's colorScheme + typography
import androidx.compose.material3.OutlinedTextField          // a single-line text input with an outline
import androidx.compose.material3.Scaffold                   // standard screen frame (handles insets / system bars)
import androidx.compose.material3.Switch                     // an on/off toggle control (used by FavoriteToggle)
import androidx.compose.material3.Text                       // draws a string using a typography style + color

// --- Compose runtime / state imports -----------------------------------------
import androidx.compose.runtime.Composable                   // marks a function as one that EMITS UI
import androidx.compose.runtime.getValue                     // enables `by` delegate READS of a State<T>
import androidx.compose.runtime.mutableStateOf               // creates observable state Compose can watch
import androidx.compose.runtime.saveable.rememberSaveable    // remembers state ACROSS recomposition AND rotation
import androidx.compose.runtime.setValue                     // enables `by` delegate WRITES to a MutableState<T>

// --- Compose UI / tooling imports --------------------------------------------
import androidx.compose.ui.Alignment                         // describes how to align children (e.g. CenterVertically)
import androidx.compose.ui.Modifier                          // the "how to size / decorate / position" object
import androidx.compose.ui.tooling.preview.Preview           // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                           // density-independent pixel unit (e.g. 16.dp)

// --- App theme import ---------------------------------------------------------
import com.example.composemodernui.ui.theme.ComposeModernUITheme // our Material 3 theme wrapper (see ui/theme/Theme.kt)

// ===========================================================================
// DATA
// ---------------------------------------------------------------------------
// A tiny in-memory model for the "notes" domain. There is NO database and NO
// network here — Compose fundamentals are about UI, not persistence. A plain
// `data class` gives us a value type with generated equals()/hashCode()/copy(),
// which is exactly what Compose wants for comparing and re-rendering data.
// ===========================================================================

/**
 * A single note shown in the list.
 *
 * @property id    a stable, unique identifier (used as the LazyColumn item key).
 * @property title the short headline shown in bold at the top of the card.
 * @property body  the one-line description shown beneath the title.
 */
data class Note(
    val id: Int,        // stable unique id — lets LazyColumn track items efficiently
    val title: String,  // short headline
    val body: String,   // one-line description
)

// A few seed notes so the screen has something to show on first launch. In a
// real app this would come from a repository/ViewModel; a hardcoded list is
// enough to demonstrate layout, theming, and reusable components.
private val seedNotes = listOf(
    Note(1, "Buy groceries", "Milk, eggs, bread, and coffee for the week."),
    Note(2, "Compose study", "Re-read the docs on state hoisting and recomposition."),
    Note(3, "Call the dentist", "Book the six-month cleaning appointment."),
)

// ===========================================================================
// UI — REUSABLE COMPONENTS
// ---------------------------------------------------------------------------
// Each component below is its OWN small @Composable with a single job. Building
// the screen out of these pieces (instead of one giant function) is what makes
// Compose UIs readable, testable, and previewable in isolation.
// ===========================================================================

/**
 * SectionHeader — a small, bold label that introduces a group of content.
 *
 * A trivial but illustrative reusable component: it reads a TEXT STYLE from
 * [MaterialTheme.typography] and a COLOR from [MaterialTheme.colorScheme], so it
 * automatically matches the app's design system everywhere it's used.
 *
 * @param text     the heading to display.
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        // titleSmall is a TYPE-SCALE role from the theme, NOT a hardcoded size —
        // change the theme's typography and every SectionHeader updates at once.
        style = MaterialTheme.typography.titleSmall,
        // Pull a semantic color from the scheme (the accent role) rather than a
        // raw hex value, so light/dark + dynamic color all "just work".
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp),         // small breathing room above/below
    )
}

/**
 * PrimaryButton — a filled Material 3 button with our standard look.
 *
 * This is a STATELESS, EVENT-EMITTING component: it holds no state of its own
 * and simply reports taps by invoking the [onClick] lambda the caller gave it.
 * That is the "events flow UP" half of state hoisting in its simplest form.
 *
 * @param text     the label shown inside the button.
 * @param onClick  invoked when the user taps the button (event flows UP to caller).
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,                                      // EVENT UP: the button never decides what a tap means
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, modifier = modifier) {          // delegate the tap straight to the caller
        Text(text)                                            // Button's content slot holds whatever UI we put here
    }
}

/**
 * NoteCard — renders a single [Note] inside a Material 3 [Card].
 *
 * Demonstrates: a Card surface, two stacked Text elements using DIFFERENT
 * typography roles, and color taken from the theme. It is STATELESS — give it a
 * note and it draws it; it owns nothing and decides nothing.
 *
 * @param note     the note to display.
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun NoteCard(note: Note, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {                // Card = rounded, elevated surface spanning the width
        // Column stacks the title above the body. Padding INSIDE the card keeps
        // the text off the card's edges.
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium, // emphasized headline role
            )
            Spacer(modifier = Modifier.height(4.dp))          // a tiny fixed gap between title and body
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,  // calmer body role
                color = MaterialTheme.colorScheme.onSurfaceVariant, // slightly muted, still theme-driven
            )
        }
    }
}

// ===========================================================================
// STATE — THE STATE-HOISTING DEMO
// ---------------------------------------------------------------------------
// FavoriteToggle is the centerpiece lesson. It is intentionally STATELESS:
//
//   • It receives the CURRENT value (isFavorite) as a PARAMETER  -> state DOWN.
//   • It reports the user's intent to change it via onToggle()    -> event UP.
//   • It stores NOTHING itself. The PARENT owns the real state.
//
// WHY HOIST? Compare the two designs:
//
//   STATEFUL (anti-pattern for reuse): the toggle keeps its own
//       `var checked by remember { mutableStateOf(false) }`.
//     - The parent can't read or control the value.
//     - You can't preview a "favorited" state with fake data.
//     - Two copies can silently disagree; testing is awkward.
//
//   STATELESS (what we do here): the value lives in the PARENT; the child is a
//   pure function of its inputs.
//     - REUSE: drop the same toggle anywhere and wire it to any state source.
//     - TESTABILITY: call it with isFavorite = true and assert what it draws.
//     - PREVIEW: render both on/off states by passing literals, no ViewModel.
//
// Rule of thumb: hoist state to the LOWEST common ancestor that needs it.
// ===========================================================================

/**
 * FavoriteToggle — a STATELESS on/off control whose state is OWNED BY THE PARENT.
 *
 * @param isFavorite the current value, passed DOWN from whoever owns the state.
 * @param onToggle   invoked when the user flips the switch; the new desired value
 *                   is sent UP so the parent (the single source of truth) updates.
 * @param modifier   optional layout modifier supplied by the caller.
 */
@Composable
fun FavoriteToggle(
    isFavorite: Boolean,                                      // STATE DOWN: we are TOLD the value; we don't store it
    onToggle: (Boolean) -> Unit,                             // EVENT UP: we report the user's requested new value
    modifier: Modifier = Modifier,
) {
    // Row lays out the label and the Switch side-by-side. spacedBy puts a fixed
    // gap between them; CenterVertically aligns them on the same baseline.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),  // even gap between label and switch
        verticalAlignment = Alignment.CenterVertically,       // vertically center both children
    ) {
        Text(
            text = if (isFavorite) "Favorited" else "Not a favorite", // text DERIVED from the hoisted value
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = isFavorite,                             // the Switch displays the PARENT'S value...
            onCheckedChange = onToggle,                       // ...and forwards changes straight back UP
        )
    }
}

// ===========================================================================
// STATE — THE SCREEN (two overloads: STATELESS + STATEFUL)
// ---------------------------------------------------------------------------
// We split the screen on purpose:
//
//   • NotesScreen(STATELESS)  — pure parameters + callbacks. This is the one we
//     compose, test, and @Preview. It never calls remember{}.
//   • NotesScreen(STATEFUL)   — the thin wrapper that OWNS the state with
//     rememberSaveable and forwards it into the stateless overload.
//
// This is the canonical "state holder vs. stateless content" separation.
// ===========================================================================

/**
 * NotesScreen (STATELESS) — pure UI: it draws exactly what it is handed and
 * reports every user action through a callback. It owns NO state and is the
 * version we preview and (could) unit-test.
 *
 * @param notes            the notes to list.
 * @param newTitle         the CURRENT text in the input box (hoisted state DOWN).
 * @param onTitleChange    called on every keystroke with the new text (event UP).
 * @param onAddNote        called when the user taps "Add note" (event UP).
 * @param isFavorite       the CURRENT favorite value (hoisted state DOWN).
 * @param onToggleFavorite called with the requested favorite value (event UP).
 * @param modifier         optional layout modifier supplied by the caller.
 */
@Composable
fun NotesScreen(
    notes: List<Note>,
    newTitle: String,                                        // STATE DOWN: the live text field value
    onTitleChange: (String) -> Unit,                        // EVENT UP: every keystroke
    onAddNote: () -> Unit,                                  // EVENT UP: "add" was tapped
    isFavorite: Boolean,                                    // STATE DOWN: the toggle value
    onToggleFavorite: (Boolean) -> Unit,                   // EVENT UP: toggle was flipped
    modifier: Modifier = Modifier,
) {
    // The outer Column stacks the whole screen vertically: header, input row,
    // favorite toggle, then the scrolling list. 16.dp padding frames the content.
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        // --- Section 1: a reusable header -----------------------------------
        SectionHeader(text = "Add a note")

        // --- Section 2: hoisted text input (event-down / state-up) ----------
        // The OutlinedTextField does NOT store its own text. Its `value` is the
        // hoisted `newTitle` and every edit calls `onValueChange` so the OWNER
        // updates the single source of truth. This is state hoisting on an input.
        OutlinedTextField(
            value = newTitle,                               // STATE DOWN: what to display right now
            onValueChange = onTitleChange,                  // EVENT UP: user typed something
            label = { Text("Note title") },                 // floating label (its own little @Composable slot)
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))           // fixed gap before the button

        // Reuse the PrimaryButton component; the tap just bubbles up via onAddNote.
        PrimaryButton(text = "Add note", onClick = onAddNote)

        Spacer(modifier = Modifier.height(24.dp))           // larger gap separating sections

        // --- Section 3: the hoisted FavoriteToggle --------------------------
        SectionHeader(text = "Preferences")
        FavoriteToggle(isFavorite = isFavorite, onToggle = onToggleFavorite)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Section 4: the scrolling list of notes -------------------------
        SectionHeader(text = "Your notes")
        // LazyColumn only composes the rows on screen. We use `key = { it.id }`
        // so Compose can track each item across list changes. spacedBy adds even
        // vertical gaps between the cards without manual Spacers.
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes, key = { it.id }) { note ->          // one NoteCard per note
                NoteCard(note = note)                        // reuse the stateless card component
            }
        }
    }
}

/**
 * NotesScreen (STATEFUL) — the STATE OWNER. This thin wrapper holds the screen's
 * state and forwards it into the stateless overload above.
 *
 * It is the ONLY place that calls remember/rememberSaveable for this screen, so
 * there is a single source of truth. `rememberSaveable` (rather than plain
 * `remember`) means the typed title and the favorite flag SURVIVE a screen
 * rotation or low-memory recreation — try rotating the device to see it persist.
 *
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun NotesScreen(modifier: Modifier = Modifier) {
    // --- OWNED STATE #1: the list of notes (seeded, then appended to) --------
    // `by ... mutableStateOf(...)` makes this observable: writing to `notes`
    // schedules a recomposition of anything that reads it.
    var notes by rememberSaveable { mutableStateOf(seedNotes) }

    // --- OWNED STATE #2: the live text in the input box ----------------------
    var newTitle by rememberSaveable { mutableStateOf("") }

    // --- OWNED STATE #3: the favorite flag hoisted out of FavoriteToggle -----
    var isFavorite by rememberSaveable { mutableStateOf(false) }

    // Hand the state DOWN and the event-handlers (which WRITE the state) into
    // the stateless overload. All the actual logic lives here, in the owner.
    NotesScreen(
        notes = notes,
        newTitle = newTitle,
        onTitleChange = { typed -> newTitle = typed },       // keystroke -> update the owned text state
        onAddNote = {
            // Only add non-blank titles; build a new immutable list and clear input.
            val trimmed = newTitle.trim()
            if (trimmed.isNotEmpty()) {
                val nextId = (notes.maxOfOrNull { it.id } ?: 0) + 1 // simple unique id
                notes = notes + Note(nextId, trimmed, "Added just now.")
                newTitle = ""                                // reset the field after adding
            }
        },
        isFavorite = isFavorite,
        onToggleFavorite = { requested -> isFavorite = requested }, // toggle -> update the owned flag
        modifier = modifier,
    )
}

/**
 * MainActivity — the app's single Activity and Android's entry point.
 *
 * It installs the Compose UI, applies the app theme, and uses a Scaffold to
 * provide the standard screen frame (and the inset padding for system bars).
 */
class MainActivity : ComponentActivity() {
    // onCreate runs once when the Activity is created; here we set the Compose UI.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)                   // always call through to the framework first
        enableEdgeToEdge()                                   // draw under the system bars for a modern edge-to-edge look
        setContent {                                         // everything inside is the Compose UI tree
            ComposeModernUITheme {                           // apply our Material 3 colors + typography
                // Scaffold gives us the standard structure and `innerPadding` —
                // the space taken by the system bars — so content isn't hidden.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Wrap the STATEFUL screen in a Box just to apply the inset
                    // padding; the stateful screen owns all UI state from here.
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NotesScreen()                        // <- the STATEFUL overload (owns the state)
                    }
                }
            }
        }
    }
}

// ===========================================================================
// @Preview functions — render UI in Android Studio's design pane.
// ---------------------------------------------------------------------------
// IMPORTANT TEACHING POINT: previews call the STATELESS NotesScreen overload and
// pass HAND-SUPPLIED data + no-op callbacks ({}). We never construct the stateful
// version (or a ViewModel) in a @Preview — previews can't run real state/logic,
// and feeding fake inputs is the whole payoff of state hoisting. Because the
// favorite value is just a parameter, we can preview BOTH on and off states.
//
// widthDp/heightDp give each preview a small phone-shaped frame so the
// fillMaxSize layout renders as a compact card in the design pane.
// ===========================================================================

// Preview A — the screen with the favorite toggle OFF and some seed notes.
@Preview(name = "Notes (not favorite)", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenNotFavoritePreview() {
    ComposeModernUITheme {                                   // previews must be wrapped in the theme too
        NotesScreen(
            notes = seedNotes,                               // hand-supplied fake data
            newTitle = "",                                   // empty input box
            onTitleChange = {},                              // no-op: previews don't react to events
            onAddNote = {},
            isFavorite = false,                              // OFF state
            onToggleFavorite = {},
        )
    }
}

// Preview B — the SAME stateless screen but with the favorite toggle ON and a
// title half-typed, proving we can render any state purely by passing values.
@Preview(name = "Notes (favorited)", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenFavoritedPreview() {
    ComposeModernUITheme {
        NotesScreen(
            notes = seedNotes,
            newTitle = "Water the plants",                   // a half-typed title, supplied by hand
            onTitleChange = {},
            onAddNote = {},
            isFavorite = true,                               // ON state — only possible because it's hoisted
            onToggleFavorite = {},
        )
    }
}

// Preview C — a single reusable component in isolation (a NoteCard). Previewing
// one component at a time is a direct benefit of breaking UI into small pieces.
@Preview(name = "NoteCard", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NoteCardPreview() {
    ComposeModernUITheme {
        NoteCard(note = Note(99, "Sample note", "This is what a single card looks like."))
    }
}
