// =============================================================================
// NotesViewModel.kt  —  UI STATE: Loading / Success / Error via StateFlow
//
// CONCEPT THIS FILE TEACHES: a network screen is never just "the data". At any moment
// it is in ONE of three states — still LOADING, loaded SUCCESS(with data), or failed
// ERROR(with a message). Model those states explicitly with a SEALED INTERFACE, expose
// the current one as a StateFlow, and the UI becomes a pure function of that state.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. NotesUiState — a sealed interface with exactly three cases.
//   2. _uiState (MutableStateFlow, private) vs uiState (StateFlow, public, read-only).
//   3. loadNotes() — launches in viewModelScope, sets Loading, then try/catch sets
//      Success or Error. The try/catch is THE error-handling boundary.
// =============================================================================

// Package declaration: ties this file to the app's namespace + directory layout.
package com.example.networkparsing

// Base class that survives configuration changes and owns viewModelScope.
import androidx.lifecycle.ViewModel
// The coroutine scope tied to this ViewModel's lifetime (auto-cancelled on clear()).
import androidx.lifecycle.viewModelScope
// Read/write reactive state holder backing the screen.
import kotlinx.coroutines.flow.MutableStateFlow
// Read-only view of the state exposed to the UI.
import kotlinx.coroutines.flow.StateFlow
// Exposes the mutable flow as an immutable StateFlow (encapsulation).
import kotlinx.coroutines.flow.asStateFlow
// Starts a coroutine to do the (suspending) network load without blocking.
import kotlinx.coroutines.launch

// ===========================================================================
// STATE  —  the three things the screen can be, made into a closed type
//
// A SEALED interface means the compiler knows the COMPLETE set of subtypes. A `when`
// over a NotesUiState can therefore be exhaustive (no `else` branch needed), so it is
// impossible to forget to render one of the states.
// ===========================================================================

/**
 * NotesUiState — the complete set of states the notes screen can be in.
 * Exactly one of these is active at any time.
 */
sealed interface NotesUiState {
    /** The request is in flight; show a spinner. (A singleton — no data to carry.) */
    data object Loading : NotesUiState

    /**
     * The request succeeded.
     * @property notes the parsed notes to render in the list.
     */
    data class Success(val notes: List<Note>) : NotesUiState

    /**
     * The request failed.
     * @property message a human-readable reason to show alongside a Retry button.
     */
    data class Error(val message: String) : NotesUiState
}

// ===========================================================================
// VIEWMODEL  —  owns the state and the loading logic
// ===========================================================================

/**
 * NotesViewModel — loads notes from a [NoteRepository] and publishes the current
 * [NotesUiState] as a [StateFlow] the screen observes.
 *
 * The repository is injected (defaulting to the offline fake) so the ViewModel never
 * hard-codes WHERE notes come from — and so tests can pass a controlled fake.
 *
 * @param repository the data source; defaults to the offline [provideNoteRepository].
 */
class NotesViewModel(
    private val repository: NoteRepository = provideNoteRepository(),
) : ViewModel() {

    // PRIVATE mutable state: only the ViewModel may change it. Seeded with Loading
    // because the screen begins fetching immediately (see init).
    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)

    // PUBLIC read-only state: the screen collects this but cannot mutate it. This
    // one-way exposure (private mutable -> public immutable) is the standard pattern.
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    // Kick off the first load as soon as the ViewModel is created.
    init {
        loadNotes()
    }

    /**
     * (Re)load notes. Safe to call again — e.g. from the Retry button after an error.
     *
     * Flow: set Loading -> try the (suspending) repository call -> on success publish
     * Success(notes); on ANY exception publish Error(message). This try/catch is the
     * single place network failures are turned into a visible UI state.
     */
    fun loadNotes() {
        // Launch on viewModelScope so the coroutine is cancelled if the VM is cleared,
        // and so the suspending getNotes() never blocks the main thread.
        viewModelScope.launch {
            _uiState.value = NotesUiState.Loading           // 1) show the spinner
            try {
                val notes = repository.getNotes()           // 2) suspend: network/parse
                _uiState.value = NotesUiState.Success(notes) // 3a) publish the data
            } catch (e: Exception) {
                // 3b) ERROR HANDLING: any failure (no network, bad JSON, timeout, ...)
                // becomes an Error state with a readable message instead of a crash.
                _uiState.value = NotesUiState.Error(
                    e.message ?: "Something went wrong while loading notes."
                )
            }
        }
    }
}
