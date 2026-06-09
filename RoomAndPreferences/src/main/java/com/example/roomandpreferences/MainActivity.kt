// =============================================================================
// MainActivity.kt  —  THE UI for the Room + DataStore persistence demo
//
// CONCEPT: LOCAL PERSISTENCE end-to-end.
//   • Room        persists STRUCTURED data — the notes list (see data/Note.kt,
//                 data/NoteDao.kt, data/NoteDatabase.kt).
//   • DataStore   persists SIMPLE settings — a dark-theme boolean + a sort order
//                 (see data/SettingsRepository.kt). DataStore REPLACES SharedPreferences.
//   • NotesViewModel exposes both as StateFlows and provides the write actions.
//
// Everything you add or toggle here SURVIVES APP RESTART, because it is written to
// the SQLite file ("notes.db") and the DataStore file ("settings"), not just held
// in memory. Kill the app and relaunch: your notes and your dark-theme choice remain.
//
// WHAT TO INSPECT HERE:
//   • collectAsStateWithLifecycle() — turns the ViewModel's persistence StateFlows
//        into Compose state that the UI reads.
//   • The dark-theme Switch wired to viewModel.setDarkTheme(...) -> DataStore.
//   • The stateless NotesScreen overload, used by @Preview (NEVER build a ViewModel
//        in a preview — see the previews at the bottom for why).
//   • Inspect the actual DB with Android Studio: View > Tool Windows > App Inspection
//        > Database Inspector, open notes.db, watch rows appear live as you add notes.
// =============================================================================
package com.example.roomandpreferences

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                       // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                     // base Activity class with Compose support
import androidx.activity.compose.setContent                    // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                       // lets the app draw behind the system bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.layout.Arrangement          // spacing strategy for Row/Column children
import androidx.compose.foundation.layout.Column               // stacks children vertically
import androidx.compose.foundation.layout.Row                  // lays out children horizontally
import androidx.compose.foundation.layout.Spacer               // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize          // modifier: take all available width AND height
import androidx.compose.foundation.layout.fillMaxWidth         // modifier: take all available width
import androidx.compose.foundation.layout.height               // modifier: force a specific height
import androidx.compose.foundation.layout.padding              // modifier: add space around content
import androidx.compose.foundation.layout.width                // modifier: force a specific width
import androidx.compose.foundation.lazy.LazyColumn             // scrolling list (only renders visible rows)
import androidx.compose.foundation.lazy.items                  // iterate a List inside a LazyColumn

// --- Material 3 component imports ---------------------------------------------
import androidx.compose.material3.Button                       // filled, tappable button
import androidx.compose.material3.Checkbox                     // square check control (note "done")
import androidx.compose.material3.HorizontalDivider            // thin horizontal separator line
import androidx.compose.material3.MaterialTheme                // access to the current theme's colors/typography
import androidx.compose.material3.OutlinedTextField            // bordered text input field
import androidx.compose.material3.Scaffold                     // standard screen frame (handles insets, bars)
import androidx.compose.material3.Switch                       // on/off toggle (wired to the DataStore dark-theme)
import androidx.compose.material3.Text                         // draws text

// --- Compose runtime / state imports -----------------------------------------
import androidx.compose.runtime.Composable                     // marks a function as emitting UI
import androidx.compose.runtime.getValue                       // property-delegate read for State<T>
import androidx.compose.runtime.mutableStateOf                 // creates observable local UI state
import androidx.compose.runtime.remember                       // remembers a value across recompositions
import androidx.compose.runtime.setValue                       // property-delegate write for MutableState<T>
import androidx.compose.ui.Alignment                           // vertical/horizontal alignment within Row/Column
import androidx.compose.ui.Modifier                            // the "how to lay out / decorate" object
import androidx.compose.ui.tooling.preview.Preview             // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                             // density-independent pixel unit (e.g. 16.dp)

// --- Lifecycle + ViewModel imports -------------------------------------------
import androidx.lifecycle.compose.collectAsStateWithLifecycle  // collects a Flow into Compose state, lifecycle-aware
import androidx.lifecycle.viewmodel.compose.viewModel          // obtains/remembers a ViewModel inside a composable

// --- App imports --------------------------------------------------------------
import com.example.roomandpreferences.data.Note                // the persisted note model (a Room row)
import com.example.roomandpreferences.data.SortOrder           // sort-order setting persisted in DataStore
import com.example.roomandpreferences.ui.theme.RoomAndPreferencesTheme // our Material theme wrapper (Theme.kt)

/**
 * MainActivity — the app's single entry-point Activity.
 *
 * It reads the persisted dark-theme setting from the [NotesViewModel] and uses it
 * to drive [RoomAndPreferencesTheme], so the whole app re-colors the instant the
 * DataStore value changes (and starts in the saved mode on next launch).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)                     // always call through to the framework first
        enableEdgeToEdge()                                     // draw under the system bars for a modern look
        setContent {
            // Obtain the ViewModel (constructed via its Application factory).
            val viewModel: NotesViewModel = viewModel(
                factory = NotesViewModel.factory(application)
            )

            // STATE: read the persisted dark-theme flag from DataStore (via the VM).
            // collectAsStateWithLifecycle keeps collection tied to the UI lifecycle.
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()

            // Drive the whole theme from the PERSISTED setting. dynamicColor=false so
            // the dark/light switch is obvious on Android 12+ (no wallpaper override).
            RoomAndPreferencesTheme(darkTheme = darkTheme, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Hand the persisted state + actions to the stateful screen wrapper.
                    NotesRoute(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

// ===========================================================================
// UI  (STATEFUL wrapper)
// ===========================================================================

/**
 * Stateful entry point: collects the ViewModel's persistence StateFlows and forwards
 * the data + callbacks to the STATELESS [NotesScreen].
 *
 * Splitting "stateful wrapper" from "stateless screen" is the standard Compose
 * pattern: the screen stays previewable and testable (no ViewModel needed), while
 * this thin layer does the wiring.
 *
 * @param viewModel the persistence-backed state holder.
 * @param modifier  layout modifier from the Scaffold (applies system-bar insets).
 */
@Composable
fun NotesRoute(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
) {
    // Collect each persistence Flow into Compose state (Room notes + DataStore settings).
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    NotesScreen(
        notes = notes,
        darkTheme = darkTheme,
        sortOrder = sortOrder,
        onAddNote = viewModel::addNote,                        // -> Room insert (persisted)
        onDeleteNote = viewModel::deleteNote,                  // -> Room delete (persisted)
        onToggleDone = viewModel::toggleDone,                  // -> Room update (persisted)
        onToggleDarkTheme = viewModel::setDarkTheme,           // -> DataStore write (persisted)
        onCycleSortOrder = {
            // Flip between the two persisted sort orders.
            viewModel.setSortOrder(
                if (sortOrder == SortOrder.NEWEST_FIRST) SortOrder.TITLE_ASC
                else SortOrder.NEWEST_FIRST
            )
        },
        modifier = modifier,
    )
}

// ===========================================================================
// UI  (STATELESS screen — fully previewable)
// ===========================================================================

/**
 * The notes screen, written as a STATELESS composable: it receives all data and
 * all callbacks as parameters and holds NO ViewModel. This is what makes it usable
 * from @Preview with hand-supplied data.
 *
 * @param notes             the persisted notes to render (from the Room Flow).
 * @param darkTheme         the persisted dark-theme flag (from DataStore).
 * @param sortOrder         the persisted sort order (from DataStore).
 * @param onAddNote         called with (title, body) to persist a new note.
 * @param onDeleteNote      called to delete a persisted note.
 * @param onToggleDone      called to flip a note's done flag (persisted).
 * @param onToggleDarkTheme called with the new dark-theme value (persisted to DataStore).
 * @param onCycleSortOrder  called to switch the persisted sort order.
 * @param modifier          optional layout modifier.
 */
@Composable
fun NotesScreen(
    notes: List<Note>,
    darkTheme: Boolean,
    sortOrder: SortOrder,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleDone: (Note) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onCycleSortOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // LOCAL, NON-PERSISTED UI state: the text currently typed into the inputs.
    // (Only the SUBMITTED note is persisted — these fields reset after adding.)
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        // --- Settings row: the DataStore-backed dark-theme Switch + sort toggle ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            // THE SETTINGS CONCEPT: toggling this Switch calls onToggleDarkTheme,
            // which writes to DataStore. The value is read back on next launch.
            Switch(
                checked = darkTheme,                           // reflects the PERSISTED value
                onCheckedChange = onToggleDarkTheme,           // -> DataStore edit{} (persisted)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Cycle the persisted sort order; the Room query Flow switches accordingly.
            Button(onClick = onCycleSortOrder) {
                Text(
                    text = when (sortOrder) {
                        SortOrder.NEWEST_FIRST -> "Sort: Newest"
                        SortOrder.TITLE_ASC -> "Sort: A-Z"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Add-note inputs ---------------------------------------------------
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onAddNote(title, body)                         // -> persisted via Room insert
                title = ""                                     // clear the local input fields
                body = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add note")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Persisted notes (${notes.size})",          // count proves the Room rows survive restart
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider()

        // --- The persisted notes list -----------------------------------------
        if (notes.isEmpty()) {
            // Friendly empty state shown on a fresh install (or after deleting all).
            Text(
                text = "No notes yet. Add one above — it will survive an app restart.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notes, key = { it.id }) { note ->         // stable key = the Room primary key
                    NoteRow(
                        note = note,
                        onToggleDone = { onToggleDone(note) },
                        onDelete = { onDeleteNote(note) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * One row in the persisted-notes list: a done Checkbox, the title/body, and a
 * Delete button. Stateless — all behavior is delegated to the passed-in callbacks.
 *
 * @param note         the persisted note this row renders.
 * @param onToggleDone called when the Checkbox is tapped (persists via Room update).
 * @param onDelete     called when Delete is tapped (persists via Room delete).
 */
@Composable
fun NoteRow(
    note: Note,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Done flag — flipping it triggers a persisted Room UPDATE.
        Checkbox(checked = note.done, onCheckedChange = { onToggleDone() })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {               // weight => take remaining width
            Text(text = note.title, style = MaterialTheme.typography.titleMedium)
            if (note.body.isNotBlank()) {
                Text(text = note.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        // Delete — triggers a persisted Room DELETE for this row's primary key.
        Button(onClick = onDelete) { Text("Delete") }
    }
}

// ===========================================================================
// @Preview functions — render the screen WITHOUT a device or a real database.
//
// IMPORTANT: previews call the STATELESS NotesScreen with HAND-SUPPLIED data and
// no-op callbacks ({}). We do NOT construct a NotesViewModel in a preview — a
// ViewModel would try to open the Room database / DataStore, which is not available
// in the design-pane environment and would make the preview fail. Previewing the
// stateless overload keeps rendering pure and instant.
// ===========================================================================

// A couple of fake notes, used only by the previews below.
private val previewNotes = listOf(
    Note(id = 1, title = "Buy groceries", body = "Milk, eggs, bread", done = false),
    Note(id = 2, title = "Read Room docs", body = "Focus on @Dao Flow queries", done = true),
    Note(id = 3, title = "Try Database Inspector", body = "App Inspection tool window", done = false),
)

// Light-theme preview with several persisted-looking notes.
@Preview(name = "Notes - Light", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenLightPreview() {
    RoomAndPreferencesTheme(darkTheme = false, dynamicColor = false) {
        NotesScreen(
            notes = previewNotes,
            darkTheme = false,
            sortOrder = SortOrder.NEWEST_FIRST,
            onAddNote = { _, _ -> },                           // no-op: previews don't persist
            onDeleteNote = {},
            onToggleDone = {},
            onToggleDarkTheme = {},
            onCycleSortOrder = {},
        )
    }
}

// Dark-theme preview (mirrors the DataStore dark-theme setting being ON).
@Preview(name = "Notes - Dark", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenDarkPreview() {
    RoomAndPreferencesTheme(darkTheme = true, dynamicColor = false) {
        NotesScreen(
            notes = previewNotes,
            darkTheme = true,
            sortOrder = SortOrder.TITLE_ASC,
            onAddNote = { _, _ -> },
            onDeleteNote = {},
            onToggleDone = {},
            onToggleDarkTheme = {},
            onCycleSortOrder = {},
        )
    }
}
