// =============================================================================
// MainActivity.kt  —  rendering LOADING / SUCCESS / ERROR from a StateFlow
//
// CONCEPT THIS PROJECT TEACHES: NETWORKING + JSON PARSING with Retrofit +
// kotlinx.serialization, surfaced as explicit LOADING / SUCCESS / ERROR UI states.
// This file is the UI layer; the data layer is split across these companions:
//   • Note.kt           — NoteDto (@Serializable JSON shape) + Note (domain) + mapper.
//   • NoteApi.kt        — the Retrofit interface and the configured Retrofit instance.
//   • NoteRepository.kt — REAL (network) vs FAKE (offline) repositories + the switch.
//   • NotesViewModel.kt — NotesUiState sealed interface + StateFlow + load logic.
//
// WHAT THE STUDENT SHOULD INSPECT IN THIS FILE:
//   1. NotesScreen() — obtains the ViewModel and collects its StateFlow with
//      collectAsStateWithLifecycle() (lifecycle-aware observation).
//   2. NotesContent() — a STATELESS overload that just renders a given NotesUiState.
//      A `when` over the sealed state draws the spinner / list / error+Retry.
//   3. The @Preview functions preview NotesContent with HAND-MADE states (never a
//      real ViewModel) — see the comment on why.
// =============================================================================

// Package declaration: ties this file to the app's namespace + directory layout.
package com.example.networkparsing

// --- Android framework imports ------------------------------------------------
import android.os.Bundle                                    // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                  // base Activity class with Compose support
import androidx.activity.compose.setContent                 // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                    // lets the app draw behind the system bars

// --- Compose layout / foundation imports -------------------------------------
import androidx.compose.foundation.layout.Arrangement       // controls spacing between children in a row/column
import androidx.compose.foundation.layout.Column            // stacks children vertically
import androidx.compose.foundation.layout.Spacer            // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize       // modifier: take all available width AND height
import androidx.compose.foundation.layout.fillMaxWidth      // modifier: take all available width
import androidx.compose.foundation.layout.height            // modifier: force a specific height
import androidx.compose.foundation.layout.padding           // modifier: add space around content
import androidx.compose.foundation.lazy.LazyColumn          // scrolling list (only renders visible rows)
import androidx.compose.foundation.lazy.items               // iterate a List inside a LazyColumn

// --- Material 3 component imports ---------------------------------------------
import androidx.compose.material3.Button                    // filled, tappable button (Retry)
import androidx.compose.material3.CircularProgressIndicator // the spinner shown during Loading
import androidx.compose.material3.HorizontalDivider         // thin horizontal separator line
import androidx.compose.material3.MaterialTheme             // access to the current theme's colors/typography
import androidx.compose.material3.Scaffold                  // standard screen frame (handles insets, bars, etc.)
import androidx.compose.material3.Text                      // draws text

// --- Compose runtime / layout imports ----------------------------------------
import androidx.compose.runtime.Composable                  // marks a function as emitting UI
import androidx.compose.runtime.getValue                    // enables `val x by …` delegation for State
import androidx.compose.ui.Alignment                        // align children (e.g. center the spinner)
import androidx.compose.ui.Modifier                         // the "how to lay out / decorate" object
import androidx.compose.ui.tooling.preview.Preview          // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                          // density-independent pixel unit (e.g. 16.dp)

// --- ViewModel + lifecycle-aware state collection ----------------------------
import androidx.lifecycle.compose.collectAsStateWithLifecycle // observe a StateFlow safely w.r.t. lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel          // obtain a ViewModel from inside a @Composable

// --- App theme ---------------------------------------------------------------
import com.example.networkparsing.ui.theme.NetworkParsingTheme // our app's Material theme wrapper (see Theme.kt)

/**
 * MainActivity — the app's single Activity and the entry point Android launches.
 * It installs the Compose UI and shows the notes screen inside the app theme.
 */
class MainActivity : ComponentActivity() {
    // onCreate runs once when the Activity is first created; we install Compose here.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)                 // always call through to the framework first
        enableEdgeToEdge()                                 // draw under the status/navigation bars
        setContent {                                       // everything inside is the Compose UI
            NetworkParsingTheme {                          // apply colors/typography/dark-light
                // Scaffold gives us innerPadding (the space the system bars occupy) so
                // content is not drawn underneath the bars.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ===========================================================================
// UI  —  STATEFUL screen: gets the ViewModel, observes its StateFlow
// ===========================================================================

/**
 * NotesScreen — the STATEFUL entry composable. It:
 *   1. obtains a [NotesViewModel] (which starts loading in its init block), and
 *   2. observes the ViewModel's StateFlow lifecycle-awarely, then
 *   3. delegates ALL drawing to the stateless [NotesContent].
 *
 * Splitting "get the state" (here) from "draw the state" ([NotesContent]) is what
 * makes the screen previewable and testable without a real ViewModel.
 *
 * @param modifier layout modifier supplied by the caller (e.g. Scaffold insets).
 * @param viewModel the screen's ViewModel; defaulted so callers rarely pass it.
 */
@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel(),               // ViewModel survives recomposition/rotation
) {
    // collectAsStateWithLifecycle() observes the StateFlow but PAUSES collection when
    // the screen is not visible (STOPPED), unlike a plain collectAsState(). This is the
    // lifecycle-safe way to read a StateFlow in Compose.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Hand the current state + the retry callback to the stateless renderer.
    NotesContent(
        uiState = uiState,
        onRetry = viewModel::loadNotes,                    // Retry simply re-runs the load
        modifier = modifier,
    )
}

/**
 * NotesContent — the STATELESS renderer. Given a [NotesUiState] it draws the matching
 * UI; given [onRetry] it knows what the error screen's button should do. It holds NO
 * state and creates NO ViewModel, which is exactly why @Previews can drive it directly.
 *
 * @param uiState which of Loading/Success/Error to render.
 * @param onRetry invoked when the user taps Retry on the error state.
 * @param modifier layout modifier supplied by the caller.
 */
@Composable
fun NotesContent(
    uiState: NotesUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // THE concept made visible: one exhaustive `when` over the sealed state. Because
    // NotesUiState is sealed, the compiler guarantees every case is handled — you
    // physically cannot forget Loading, Success, or Error.
    when (uiState) {
        // LOADING — center a spinner in the available space.
        is NotesUiState.Loading -> LoadingState(modifier)

        // SUCCESS — render the parsed notes in a scrolling list.
        is NotesUiState.Success -> NotesList(notes = uiState.notes, modifier = modifier)

        // ERROR — show the message plus a Retry button wired to onRetry.
        is NotesUiState.Error -> ErrorState(
            message = uiState.message,
            onRetry = onRetry,
            modifier = modifier,
        )
    }
}

/**
 * LoadingState — a centered [CircularProgressIndicator] shown while the request runs.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,          // vertically center the spinner
    ) {
        CircularProgressIndicator()                        // <-- the Loading UI
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Loading notes…", style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * NotesList — renders the SUCCESS state: a scrolling list of [notes], one row each.
 *
 * @param notes the parsed notes to display.
 */
@Composable
private fun NotesList(notes: List<Note>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {        // only composes visible rows
        items(notes) { note ->                             // one block of UI per note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title in an emphasized type style.
                Text(text = note.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                // Body in smaller text beneath the title.
                Text(text = note.body, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()                            // separates one note from the next
        }
    }
}

/**
 * ErrorState — renders the ERROR state: the failure [message] and a Retry button.
 *
 * @param message the human-readable error from [NotesUiState.Error].
 * @param onRetry invoked when Retry is tapped (re-runs the load).
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Couldn't load notes",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        // The actual reason, surfaced from the caught exception's message.
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        // Retry: re-triggers loadNotes() in the ViewModel.
        Button(onClick = onRetry) {                        // <-- the Retry affordance
            Text("Retry")
        }
    }
}

// ===========================================================================
// PREVIEWS  —  render EACH state without a network or a real ViewModel
//
// IMPORTANT: previews drive the STATELESS NotesContent with HAND-SUPPLIED state. We
// never construct a NotesViewModel in a @Preview because a ViewModel would try to run
// a coroutine / hit the repository, which the design pane cannot do. Passing a literal
// NotesUiState renders each state instantly and deterministically.
// ===========================================================================

// A couple of fake notes used by the Success preview.
private val previewNotes = listOf(
    Note(1, "First note", "A short body for the first previewed note."),
    Note(2, "Second note", "A short body for the second previewed note."),
)

// SUCCESS state preview — the list of notes.
@Preview(name = "Success", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesContentSuccessPreview() {
    NetworkParsingTheme {
        NotesContent(
            uiState = NotesUiState.Success(previewNotes),  // hand-made Success state
            onRetry = {},                                  // no-op: previews don't navigate
        )
    }
}

// LOADING state preview — the centered spinner.
@Preview(name = "Loading", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesContentLoadingPreview() {
    NetworkParsingTheme {
        NotesContent(
            uiState = NotesUiState.Loading,                // hand-made Loading state
            onRetry = {},
        )
    }
}

// ERROR state preview — message + Retry button.
@Preview(name = "Error", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotesContentErrorPreview() {
    NetworkParsingTheme {
        NotesContent(
            uiState = NotesUiState.Error("No internet connection."), // hand-made Error state
            onRetry = {},
        )
    }
}
