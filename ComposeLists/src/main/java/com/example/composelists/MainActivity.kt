// =============================================================================
// MainActivity.kt
//
// CONCEPT: LISTS in Jetpack Compose.
//
// This teaching sample is NOT about navigation. It demonstrates everything a
// student needs to know to render and manage *collections* of items on screen:
//
//   1. LazyColumn          — a vertically scrolling list that only composes the
//                            rows currently visible (lazy = efficient, like a
//                            RecyclerView but with almost no boilerplate).
//   2. LazyVerticalGrid    — the same idea, but laid out in a grid. We use
//                            GridCells.Fixed(2) for a two-column grid.
//   3. Stable item keys    — items(list, key = { it.id }) so Compose can track
//                            item *identity* across recomposition / reorder /
//                            filtering, preserving scroll position & animations.
//   4. Multi-SELECTION     — tapping a note toggles it in a hoisted Set<Int>.
//                            Selected items render with a highlighted background.
//   5. EMPTY STATE         — a dedicated composable shown when the filter
//                            matches zero notes (a list UI is not "done" until
//                            it gracefully handles the no-results case).
//   6. Text FILTERING      — a search field whose query is hoisted state; the
//                            visible list is *derived* from (allNotes + query).
//
// WHAT THE STUDENT SHOULD INSPECT (in rough reading order):
//   • NotesViewModel       — where the source-of-truth list & query/selection
//                            state live, and how the filtered list is derived.
//   • NotesScreen          — the STATEFUL screen that owns a ViewModel and
//                            forwards its state down to the stateless content.
//   • NotesContent         — the STATELESS UI: search box, layout toggle,
//                            selection bar, and the column/grid switch.
//   • NoteCard             — a single item; note how `selected` drives the
//                            background color and how the whole card is clickable.
//   • the `key = { it.id }` argument in BOTH items(...) calls below.
//
// Reading order below: package + imports, then DATA, then STATE (ViewModel),
// then the Activity, then the UI composables, then the @Preview functions.
// =============================================================================

// The package declaration. Every class/function below lives in this namespace,
// which also matches the directory structure under src/main/java/.
package com.example.composelists

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                    // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                  // base Activity class with Compose support
import androidx.activity.compose.setContent                 // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                    // lets the app draw behind the system bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.clickable                // makes a composable (a note card) tappable
import androidx.compose.foundation.layout.Arrangement        // controls spacing between children in rows/columns
import androidx.compose.foundation.layout.Column             // stacks children vertically
import androidx.compose.foundation.layout.PaddingValues      // describes content padding for the lazy lists
import androidx.compose.foundation.layout.Row                // arranges children horizontally (the selection bar)
import androidx.compose.foundation.layout.Spacer             // an empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize        // makes a composable fill all available space
import androidx.compose.foundation.layout.fillMaxWidth       // makes a composable as wide as its parent
import androidx.compose.foundation.layout.height             // sets a fixed height (used for spacers)
import androidx.compose.foundation.layout.padding            // adds outer space around a composable

// --- Compose LAZY LIST imports (the heart of this sample) --------------------
import androidx.compose.foundation.lazy.LazyColumn           // the scrolling, lazily-composed vertical LIST
import androidx.compose.foundation.lazy.items                // the items(list, key = ...) overload for LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells       // describes how many columns a grid has
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // the lazily-composed GRID
import androidx.compose.foundation.lazy.grid.items as gridItems // items(...) for the grid (aliased to avoid a name clash)

// --- Compose shape import -----------------------------------------------------
import androidx.compose.foundation.shape.RoundedCornerShape  // rounds the corners of note cards

// --- Material 3 component imports --------------------------------------------
import androidx.compose.material3.Card                       // a Material surface for each note
import androidx.compose.material3.CardDefaults               // default elevation/colors helpers for Card
import androidx.compose.material3.ExperimentalMaterial3Api   // opt-in annotation for the TopAppBar API
import androidx.compose.material3.HorizontalDivider          // a thin separator line
import androidx.compose.material3.MaterialTheme              // exposes the app's colors & typography
import androidx.compose.material3.OutlinedTextField          // the SEARCH input field
import androidx.compose.material3.Scaffold                   // standard screen skeleton (top bar + content)
import androidx.compose.material3.Surface                    // a themed background container
import androidx.compose.material3.Text                       // renders text
import androidx.compose.material3.TextButton                 // the layout toggle + "Clear selection" actions
import androidx.compose.material3.TopAppBar                  // the app's title bar

// --- Compose runtime / state imports -----------------------------------------
import androidx.compose.runtime.Composable                   // marks a function as emitting UI
import androidx.compose.runtime.getValue                     // property-delegate read for State (the `by` keyword)
import androidx.compose.runtime.mutableStateOf               // creates observable state
import androidx.compose.runtime.setValue                     // property-delegate write for State (the `by` keyword)

// --- Compose UI plumbing ------------------------------------------------------
import androidx.compose.ui.Alignment                         // alignment for Row/Column children
import androidx.compose.ui.Modifier                          // the chainable UI-decorator object
import androidx.compose.ui.text.font.FontWeight              // bold/normal weights for text
import androidx.compose.ui.text.style.TextOverflow           // how overflowing text is truncated
import androidx.compose.ui.tooling.preview.Preview           // marks a @Preview function for the IDE
import androidx.compose.ui.unit.dp                           // density-independent pixels (sizes/spacing)

// --- ViewModel + Compose integration -----------------------------------------
import androidx.lifecycle.ViewModel                          // the lifecycle-aware state holder base class
import androidx.lifecycle.viewmodel.compose.viewModel        // obtains a ViewModel inside a Composable

// --- Project theme ------------------------------------------------------------
import com.example.composelists.ui.theme.ComposeListsTheme   // our Material 3 theme wrapper (see ui/theme/Theme.kt)

// =============================================================================
// DATA
//
// A plain immutable model class for one note. The `id` is the crucial field:
// it is a *stable identity* that survives filtering and reordering, and it is
// exactly what we hand to `key = { it.id }` in the lazy lists further below.
// =============================================================================

/**
 * A single note in the notes domain.
 *
 * @property id    a stable, unique identifier. This is what Compose uses as the
 *                 item KEY so it can track a note's identity across recomposition,
 *                 reordering, and filtering (preserving scroll & animations).
 * @property title the short headline shown in bold.
 * @property body  the longer text shown beneath the title.
 */
data class Note(
    val id: Int,        // STABLE KEY — never reused for a different note
    val title: String,  // searched against (case-insensitive) by the filter
    val body: String    // also searched against by the filter
)

/**
 * The seed data: ~12 sample notes. In a real app this would come from a
 * database or network; here it is a hard-coded list so the sample is self
 * contained. Each note has a UNIQUE [Note.id].
 */
val sampleNotes: List<Note> = listOf(
    Note(1,  "Buy groceries",      "Milk, eggs, bread, and coffee beans."),
    Note(2,  "Compose meeting",    "Discuss LazyColumn vs LazyVerticalGrid."),
    Note(3,  "Call the dentist",   "Reschedule the cleaning for next week."),
    Note(4,  "Read Kotlin book",   "Finish the coroutines chapter tonight."),
    Note(5,  "Gym session",        "Leg day: squats, lunges, and stretching."),
    Note(6,  "Plan vacation",      "Compare flights and book a hotel in June."),
    Note(7,  "Water the plants",   "The fern and the basil on the balcony."),
    Note(8,  "Fix the bug",        "Selection set was not surviving rotation."),
    Note(9,  "Write README",       "Explain stable keys and the empty state."),
    Note(10, "Renew passport",     "Appointment slots open on the first Monday."),
    Note(11, "Backup photos",      "Upload last month's pictures to the cloud."),
    Note(12, "Learn grids",        "Try GridCells.Fixed(2) for a two-up layout.")
)

// =============================================================================
// STATE (ViewModel)
//
// The ViewModel is the single source of truth for everything mutable on this
// screen: the search query, the selected ids, and the current layout mode.
// Keeping this OUTSIDE the composables ("hoisting") means the UI is a pure
// function of state, which is what makes Compose predictable and testable.
// =============================================================================

/** The two ways this screen can lay the SAME data out. */
enum class ListLayout { COLUMN, GRID }   // COLUMN = LazyColumn, GRID = LazyVerticalGrid

/**
 * Owns and exposes all mutable screen state for the notes list.
 *
 * Why a ViewModel? It survives configuration changes (e.g. rotation), so the
 * search text, the selection, and the layout choice are NOT lost when the
 * Activity is recreated.
 *
 * The list shown on screen ([visibleNotes]) is *derived* — it is never stored
 * directly; it is recomputed from [allNotes] + [query] every time it's read.
 * Derived-from-state is the idiomatic Compose way to avoid stale duplicates.
 */
class NotesViewModel : ViewModel() {

    /** The full, unfiltered source list. Private so the UI can't mutate it. */
    private val allNotes: List<Note> = sampleNotes

    /** The current search text. `by mutableStateOf` makes reads observable. */
    var query by mutableStateOf("")
        private set                                          // only this VM may write it

    /** The set of SELECTED note ids. A Set gives O(1) "is selected?" checks. */
    var selectedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    /** Which layout (column vs grid) renders the data right now. */
    var layout by mutableStateOf(ListLayout.COLUMN)
        private set

    /**
     * The list actually rendered: [allNotes] filtered by [query], matched
     * case-insensitively against BOTH the title and the body. Computed on read.
     */
    val visibleNotes: List<Note>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return allNotes                 // no query → show everything
            return allNotes.filter { note ->
                // contains(..., ignoreCase = true) is the case-insensitive match
                note.title.contains(q, ignoreCase = true) ||
                    note.body.contains(q, ignoreCase = true)
            }
        }

    /** Update the search text (called by the OutlinedTextField's onValueChange). */
    fun onQueryChange(new: String) { query = new }

    /**
     * Toggle one note's selection. Building a NEW set (rather than mutating the
     * old one) is what lets Compose detect the change and recompose.
     */
    fun toggleSelected(id: Int) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    /** Clear every selection at once (the "Clear selection" action). */
    fun clearSelection() { selectedIds = emptySet() }

    /** Flip between the LazyColumn and LazyVerticalGrid layouts. */
    fun toggleLayout() {
        layout = if (layout == ListLayout.COLUMN) ListLayout.GRID else ListLayout.COLUMN
    }
}

// =============================================================================
// ACTIVITY
//
// The single Activity. It only does two things: enable edge-to-edge drawing,
// and hand Compose the root composable wrapped in our theme.
// =============================================================================

/**
 * The app's only Activity and entry point (declared in AndroidManifest.xml).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                                   // draw behind the status/navigation bars
        setContent {                                         // attach a Compose UI tree to this Activity
            ComposeListsTheme {                              // apply Material 3 colors + typography
                // A Surface paints the themed background behind everything.
                Surface(modifier = Modifier.fillMaxSize()) {
                    NotesScreen()                            // the stateful root screen
                }
            }
        }
    }
}

// =============================================================================
// UI — STATEFUL SCREEN
//
// NotesScreen owns the ViewModel and forwards its state + callbacks down to the
// STATELESS NotesContent. Splitting "stateful screen" from "stateless content"
// is what makes the content @Preview-able without ever building a ViewModel.
// =============================================================================

/**
 * The stateful notes screen.
 *
 * Obtains a [NotesViewModel] and wires its state/events to the stateless
 * [NotesContent]. This is the only composable in the file that touches a
 * ViewModel — everything below it is a pure function of its parameters.
 */
@Composable
fun NotesScreen(viewModel: NotesViewModel = viewModel()) {
    // Read each piece of observable state. Reading inside a composable
    // SUBSCRIBES this composable to changes, so it recomposes when they change.
    NotesContent(
        query = viewModel.query,
        onQueryChange = viewModel::onQueryChange,
        notes = viewModel.visibleNotes,                      // the DERIVED, filtered list
        selectedIds = viewModel.selectedIds,
        onToggleSelected = viewModel::toggleSelected,
        onClearSelection = viewModel::clearSelection,
        layout = viewModel.layout,
        onToggleLayout = viewModel::toggleLayout
    )
}

// =============================================================================
// UI — STATELESS CONTENT
//
// Everything from here down receives state as parameters and emits events via
// lambdas. No ViewModel, no remember of business state → trivially previewable.
// =============================================================================

/**
 * The stateless notes UI: search box, layout toggle, selection bar, and the
 * column/grid switch over the SAME data.
 *
 * @param query             the current search text (hoisted above this composable).
 * @param onQueryChange     called as the user types in the search field.
 * @param notes             the already-FILTERED list to display.
 * @param selectedIds       the set of currently selected note ids.
 * @param onToggleSelected  called when a note is tapped (toggles its selection).
 * @param onClearSelection  called by the "Clear selection" action.
 * @param layout            COLUMN → LazyColumn, GRID → LazyVerticalGrid.
 * @param onToggleLayout    called by the toolbar icon to flip the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesContent(
    query: String,
    onQueryChange: (String) -> Unit,
    notes: List<Note>,
    selectedIds: Set<Int>,
    onToggleSelected: (Int) -> Unit,
    onClearSelection: () -> Unit,
    layout: ListLayout,
    onToggleLayout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Lists") },
                actions = {
                    // The LAYOUT TOGGLE. A TextButton whose label hints at what
                    // tapping will switch TO (kept text-only to stay on base
                    // Compose + Material3 with no extra material-icons dependency).
                    TextButton(onClick = onToggleLayout) {
                        Text(
                            if (layout == ListLayout.COLUMN) "Grid"  // column now → switch to grid
                            else "List"                               // grid now → switch to list
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // --- SEARCH FIELD -------------------------------------------------
            // Hoisted query state: this field is the single source of the text,
            // and the list above (`notes`) is already filtered from it.
            OutlinedTextField(
                value = query,                               // controlled by hoisted state
                onValueChange = onQueryChange,               // every keystroke updates the query
                label = { Text("Search notes") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- SELECTION BAR ------------------------------------------------
            // Shows a live selected-count and the "Clear selection" action, but
            // only when at least one note is selected.
            if (selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedIds.size} selected",   // the live count
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearSelection) {
                        Text("Clear selection")
                    }
                }
                HorizontalDivider()
            }

            // --- THE LIST / GRID ----------------------------------------------
            if (notes.isEmpty()) {
                // EMPTY STATE: the filter matched nothing. Always handle this —
                // a blank screen looks like a bug to the user.
                EmptyState(query = query)
            } else when (layout) {
                // SAME data, two different lazy layouts. Both pass key = { it.id }.
                ListLayout.COLUMN -> NotesColumn(
                    notes = notes,
                    selectedIds = selectedIds,
                    onToggleSelected = onToggleSelected
                )
                ListLayout.GRID -> NotesGrid(
                    notes = notes,
                    selectedIds = selectedIds,
                    onToggleSelected = onToggleSelected
                )
            }
        }
    }
}

/**
 * The LazyColumn rendering of the notes.
 *
 * THE concept line is `items(notes, key = { it.id })`: passing a STABLE key
 * lets Compose match each composed row to a note's identity. When the list is
 * filtered or reordered, Compose moves/keeps the right rows instead of blindly
 * recomposing by position — preserving scroll position and item animations.
 */
@Composable
fun NotesColumn(
    notes: List<Note>,
    selectedIds: Set<Int>,
    onToggleSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // items(list, key = { ... }) — STABLE KEYS. `key = { it.id }` ties each
        // row to a note's unique id, NOT its position in the list.
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                selected = note.id in selectedIds,           // O(1) membership test
                onClick = { onToggleSelected(note.id) }
            )
        }
    }
}

/**
 * The LazyVerticalGrid rendering of the SAME notes, in two fixed columns.
 *
 * Note the identical `key = { it.id }` here (via the aliased `gridItems`): the
 * grid benefits from stable keys for exactly the same reasons the column does.
 */
@Composable
fun NotesGrid(
    notes: List<Note>,
    selectedIds: Set<Int>,
    onToggleSelected: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),                        // a two-column grid
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // SAME stable-key pattern as the column — identity is tracked by id.
        gridItems(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                selected = note.id in selectedIds,
                onClick = { onToggleSelected(note.id) }
            )
        }
    }
}

/**
 * One note item, used by BOTH the column and the grid.
 *
 * @param note     the note to render.
 * @param selected whether this note is currently selected. Drives the highlight.
 * @param onClick  toggles this note's selection when the card is tapped.
 */
@Composable
fun NoteCard(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit
) {
    // SELECTION HIGHLIGHT: selected cards use the theme's "container" accent so
    // they stand out; unselected cards use the normal surface variant.
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)                    // the WHOLE card toggles selection
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis             // keep grid cells tidy
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = note.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The EMPTY STATE shown when filtering yields no notes.
 *
 * A list UI must explain *why* it is blank; here we echo the query so the user
 * understands their search matched nothing (rather than thinking the app broke).
 */
@Composable
fun EmptyState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No notes found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (query.isBlank()) "There are no notes yet."
                   else "Nothing matches \"$query\".",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// =============================================================================
// PREVIEWS
//
// Previews render the STATELESS composables with hand-supplied data. We never
// build a NotesViewModel in a @Preview, because the IDE preview environment has
// no Activity/lifecycle to host a ViewModel — and a stateless preview is also
// a clearer demonstration of "UI is a function of state".
// =============================================================================

/** Preview: the COLUMN layout with one note pre-selected. */
@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesColumnPreview() {
    ComposeListsTheme {
        NotesContent(
            query = "",
            onQueryChange = {},                              // no-op: previews are static
            notes = sampleNotes.take(5),                     // hand-supplied data
            selectedIds = setOf(2),                          // show the selection highlight
            onToggleSelected = {},
            onClearSelection = {},
            layout = ListLayout.COLUMN,
            onToggleLayout = {}
        )
    }
}

/** Preview: the GRID layout (GridCells.Fixed(2)) with two notes selected. */
@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesGridPreview() {
    ComposeListsTheme {
        NotesContent(
            query = "",
            onQueryChange = {},
            notes = sampleNotes.take(6),
            selectedIds = setOf(1, 4),
            onToggleSelected = {},
            onClearSelection = {},
            layout = ListLayout.GRID,
            onToggleLayout = {}
        )
    }
}

/** Preview: the EMPTY STATE shown when a search matches nothing. */
@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun EmptyStatePreview() {
    ComposeListsTheme {
        EmptyState(query = "xyz")
    }
}
