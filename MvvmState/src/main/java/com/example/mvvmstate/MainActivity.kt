// =============================================================================
// MainActivity.kt
//
// PROJECT: MvvmState — a teaching sample for MVVM + ViewModel + StateFlow.
//
// CONCEPT TAUGHT HERE (the UI half of the lesson):
//   • MVVM                     The Composable (View) is "dumb": it renders state
//                              and forwards events. All logic lives in the
//                              NotesViewModel (see NotesViewModel.kt).
//   • collectAsStateWithLifecycle()
//                              Subscribes to the ViewModel's StateFlow and turns
//                              it into Compose State, so the UI recomposes on every
//                              new emission — and pauses collecting when not visible.
//   • UNIDIRECTIONAL DATA FLOW The state flows DOWN (uiState -> composables) and
//                              the events flow UP (callbacks -> onEvent). The UI
//                              never mutates state directly.
//
// READING ORDER: package + imports, then the Activity, then the STATEFUL screen
// (which owns the ViewModel), then the STATELESS screen (pure UI of state +
// callbacks — also what the @Preview functions render), then the row composable,
// then the previews at the bottom.
//
// WHAT TO INSPECT (student):
//   • In NotesScreen, see how uiState is read and how every user action calls
//     viewModel.onEvent(...) — there is NO business logic in the composable.
//   • NotesScreenContent is STATELESS: it takes a NotesUiState + an onEvent
//     lambda. This is what makes it previewable and testable without a ViewModel.
//   • The @Preview functions hand-build a NotesUiState — they NEVER construct a
//     ViewModel (a preview has no real lifecycle/coroutine environment).
//   • Rotate the running app after typing a note title: the draft text and the
//     list survive, because that state lives in the retained ViewModel.
// =============================================================================
package com.example.mvvmstate                                  // every declaration below lives in this package

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                       // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                     // base Activity class with Compose support
import androidx.activity.compose.setContent                    // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                       // lets the app draw behind the system bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.layout.Arrangement          // controls spacing/centering of children in a Row/Column
import androidx.compose.foundation.layout.Column               // stacks children vertically
import androidx.compose.foundation.layout.Row                  // lays children out horizontally (used per note row)
import androidx.compose.foundation.layout.Spacer               // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize          // modifier: take all available width AND height
import androidx.compose.foundation.layout.fillMaxWidth         // modifier: take all available width
import androidx.compose.foundation.layout.height               // modifier: force a specific height
import androidx.compose.foundation.layout.padding              // modifier: add space around content
import androidx.compose.foundation.layout.width                // modifier: force a specific width
import androidx.compose.foundation.lazy.LazyColumn             // scrolling list (only renders visible rows)
import androidx.compose.foundation.lazy.items                  // iterate a List inside a LazyColumn

// --- Material 3 component imports ---------------------------------------------
import androidx.compose.material3.Button                       // filled, tappable button (the "Add" button)
import androidx.compose.material3.Checkbox                     // the per-note "done" toggle
import androidx.compose.material3.CircularProgressIndicator    // the spinner shown while isLoading is true
import androidx.compose.material3.HorizontalDivider            // thin horizontal separator line
import androidx.compose.material3.MaterialTheme                // access to the current theme's colors/typography
import androidx.compose.material3.OutlinedTextField            // the "new note" text input
import androidx.compose.material3.Scaffold                     // standard screen frame (handles insets, bars, etc.)
import androidx.compose.material3.Text                         // draws text
import androidx.compose.material3.TextButton                   // low-emphasis button (the per-note "Delete")

// --- Compose runtime / tooling imports ---------------------------------------
import androidx.compose.runtime.Composable                     // marks a function as emitting UI
import androidx.compose.runtime.getValue                       // enables `by` delegation when reading a State value
import androidx.compose.ui.Alignment                           // vertical/horizontal alignment within a Row/Column
import androidx.compose.ui.Modifier                            // the "how to lay out / decorate" object
import androidx.compose.ui.text.style.TextDecoration           // strike-through styling for "done" notes
import androidx.compose.ui.tooling.preview.Preview             // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                             // density-independent pixel unit (e.g. 16.dp)

// --- MVVM / lifecycle Compose imports (THE concept-specific imports) ----------
import androidx.lifecycle.viewmodel.compose.viewModel          // obtains a lifecycle-scoped NotesViewModel inside a Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle  // collects the StateFlow as Compose State, lifecycle-aware

// --- App imports --------------------------------------------------------------
import com.example.mvvmstate.ui.theme.MvvmStateTheme           // our app's Material theme wrapper (see Theme.kt)

/**
 * MainActivity — the app's single Activity and the entry point Android launches.
 *
 * It only installs the Compose UI tree and applies the theme. All real work
 * happens in [NotesScreen] / [NotesViewModel]; the Activity stays thin on purpose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)                     // always call through to the framework first
        enableEdgeToEdge()                                     // draw under the status/navigation bars for a modern look
        setContent {                                           // everything inside is the Compose UI
            MvvmStateTheme {                                   // apply colors + typography
                // Scaffold gives us a standard frame and the inset padding so our
                // content isn't drawn underneath the system bars.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // The stateful screen owns the ViewModel; we pass the inset padding down.
                    NotesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ===========================================================================
// UI — STATEFUL screen (owns the ViewModel; wires state DOWN and events UP)
// ===========================================================================

/**
 * NotesScreen — the STATEFUL entry point that connects the [NotesViewModel] to
 * the stateless UI.
 *
 * This is the one place that knows about the ViewModel. It does TWO things and
 * nothing else:
 *   1. Reads the ViewModel's [NotesViewModel.uiState] and collects it as Compose
 *      State (state flows DOWN).
 *   2. Forwards user actions to [NotesViewModel.onEvent] (events flow UP).
 *
 * @param modifier  layout modifier supplied by the caller (e.g. inset padding).
 * @param viewModel the screen's ViewModel; `viewModel()` returns one scoped to the
 *                  host (Activity), so it SURVIVES configuration changes (rotation).
 */
@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel(),                   // retained across rotation -> its StateFlow keeps state
) {
    // STATE DOWN: subscribe to the StateFlow. collectAsStateWithLifecycle() emits
    // a new value on every ViewModel update, triggering recomposition (UDF step 6),
    // and stops collecting while the screen is not visible.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Render the stateless UI with the current snapshot, and route every action to
    // the ViewModel's single onEvent funnel (UDF step 3). No logic lives here.
    NotesScreenContent(
        uiState = uiState,                                     // state DOWN
        onEvent = viewModel::onEvent,                          // events UP (method reference to the single funnel)
        modifier = modifier,
    )
}

// ===========================================================================
// UI — STATELESS screen (pure function of state + an event sink)
// ===========================================================================

/**
 * NotesScreenContent — a STATELESS rendering of a [NotesUiState].
 *
 * It holds no ViewModel and no remembered state of its own: give it a state and an
 * [onEvent] sink and it draws that exact state. This is what makes it:
 *   • PREVIEWABLE — the @Preview functions can hand it a fabricated state.
 *   • TESTABLE    — a UI test can assert on any state without a real ViewModel.
 *
 * @param uiState  the immutable snapshot to render (state flows DOWN to here).
 * @param onEvent  the sink every user action is reported through (events flow UP).
 * @param modifier optional layout modifier supplied by the caller.
 */
@Composable
fun NotesScreenContent(
    uiState: NotesUiState,
    onEvent: (NotesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Screen title.
        Text(text = "Notes", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // --- The "add note" input row ---------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                // The field renders the draft straight from state (DOWN). It keeps
                // no internal text of its own — the ViewModel is the source of truth.
                value = uiState.newNoteTitle,
                // Every keystroke fires an event UP; the ViewModel computes the new
                // state and emits it, and THIS field re-renders from that new state.
                onValueChange = { typed -> onEvent(NotesEvent.NewTitleChanged(typed)) },
                label = { Text("New note") },
                modifier = Modifier.weight(1f),                // take the remaining width
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                // Tapping "Add" sends the AddNote event UP — the composable does NOT
                // build a Note or touch the list itself; that is the ViewModel's job.
                onClick = { onEvent(NotesEvent.AddNote) },
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Body: either a loading spinner OR the list of notes ------------
        // The single isLoading flag in the immutable state decides which to show;
        // there is no separate mutable "loading" variable hiding in the UI.
        if (uiState.isLoading) {
            // Centered spinner while the fake repository is "loading".
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()                    // driven purely by uiState.isLoading
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading notes…")
            }
        } else {
            // The scrolling list of notes, rendered from uiState.notes.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.notes, key = { it.id }) { note ->  // one row per note; key by id for stable recomposition
                    // Each row reports its toggle/delete intents UP via onEvent.
                    NoteRow(
                        note = note,
                        onToggle = { onEvent(NotesEvent.ToggleDone(note.id)) },
                        onDelete = { onEvent(NotesEvent.DeleteNote(note.id)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * NoteRow — renders one [Note] with a done-checkbox and a delete button.
 *
 * Stateless and reusable: it does not know what "toggle" or "delete" actually do;
 * it just invokes the callbacks. The parent translates those into [NotesEvent]s.
 *
 * @param note     the immutable note to display.
 * @param onToggle invoked when the checkbox is tapped.
 * @param onDelete invoked when the delete button is tapped.
 */
@Composable
fun NoteRow(
    note: Note,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The checkbox reflects note.done (state DOWN) and reports taps UP.
        Checkbox(checked = note.done, onCheckedChange = { onToggle() })
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note.title,
            style = MaterialTheme.typography.bodyLarge,
            // A "done" note is rendered with a strike-through — derived purely from
            // the immutable state, not from any local mutable UI flag.
            textDecoration = if (note.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDelete) {                        // low-emphasis delete action
            Text("Delete")
        }
    }
}

// ===========================================================================
// PREVIEWS — render hand-built states in Android Studio's design pane.
//
// IMPORTANT: every preview targets the STATELESS NotesScreenContent and supplies
// a fabricated NotesUiState plus a no-op onEvent ({}). We NEVER construct a
// NotesViewModel in a @Preview: a preview has no real Activity lifecycle or
// coroutine/viewModelStore environment, so building one would be fragile and
// would also kick off its init{} loading coroutine. Previewing the stateless
// overload is the idiomatic, robust way to see each UI state in isolation.
// ===========================================================================

/** Preview of the loaded list, including one "done" (struck-through) note. */
@Preview(name = "Notes — loaded", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenLoadedPreview() {
    MvvmStateTheme {                                           // wrap in the app theme so colors/typography match the app
        NotesScreenContent(
            // Hand-built immutable state — no ViewModel involved.
            uiState = NotesUiState(
                notes = listOf(
                    Note(id = 1, title = "Read about ViewModel", done = true),
                    Note(id = 2, title = "Understand StateFlow", done = false),
                    Note(id = 3, title = "Draw the UDF loop", done = false),
                ),
                isLoading = false,
                newNoteTitle = "Draft note in progress",       // shows the draft surviving in state
            ),
            onEvent = {},                                      // previews don't react to events
        )
    }
}

/** Preview of the loading state — drives the spinner via isLoading = true. */
@Preview(name = "Notes — loading", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesScreenLoadingPreview() {
    MvvmStateTheme {
        NotesScreenContent(
            uiState = NotesUiState(isLoading = true),          // loading -> spinner branch
            onEvent = {},
        )
    }
}
