// =============================================================================
// NotesViewModel.kt
//
// CONCEPT FOCUS: this is the HEART of the lesson — MVVM + ViewModel + StateFlow +
// an IMMUTABLE UiState + UI EVENTS + UNIDIRECTIONAL DATA FLOW (UDF).
//
// THE UDF LOOP (read this slowly — the whole project exists to teach it):
//
//   1. UI RENDERS STATE      The screen draws whatever is in the current
//                            NotesUiState (state flows DOWN to the UI).
//   2. USER ACTION           The user types / taps a checkbox / taps delete.
//   3. onEvent(event)        The UI sends a NotesEvent UP — it does NOT mutate
//                            state itself. This is the ONLY entry point.
//   4. VIEWMODEL COMPUTES    onEvent builds a brand-NEW immutable NotesUiState
//      A NEW STATE           with .copy(...) (never edits the old one).
//   5. StateFlow EMITS       _uiState.update { ... } publishes the new state.
//   6. UI RECOMPOSES         Compose observes the StateFlow and re-renders.
//                            -> back to step 1. The data flows in ONE direction.
//
// WHY VIEWMODEL STATE SURVIVES ROTATION:
//   A rotation destroys and recreates the Activity, but the ViewModel is retained
//   by the framework across that recreation (it is scoped to the Activity/owner,
//   not to a single Activity instance). Because the authoritative NotesUiState
//   lives inside this ViewModel — not in the Composable — the typed-but-unsaved
//   title, the loaded notes, and the loading flag all SURVIVE the rotation. If we
//   had kept that state in the composable with a plain `var`, it would be lost.
//
// WHAT TO INSPECT (student):
//   • NotesUiState: every property is a `val` -> immutable; updates use .copy().
//   • NotesEvent: a sealed interface = the fixed, exhaustive list of things the
//     UI may ask for. `when (event)` over it is checked at compile time.
//   • onEvent(): the SINGLE funnel; trace each branch to a state update.
//   • _uiState (private MutableStateFlow) vs uiState (public read-only StateFlow):
//     the UI can READ but never WRITE the state directly.
// =============================================================================
package com.example.mvvmstate                                  // same package as the rest of the app

import androidx.lifecycle.ViewModel                            // base class that survives configuration changes (rotation)
import androidx.lifecycle.viewModelScope                       // a CoroutineScope cancelled automatically when the VM dies
import kotlinx.coroutines.flow.MutableStateFlow                // the writable, observable state holder (private)
import kotlinx.coroutines.flow.StateFlow                       // the read-only view of that state exposed to the UI
import kotlinx.coroutines.flow.asStateFlow                     // narrows a MutableStateFlow to a read-only StateFlow
import kotlinx.coroutines.flow.update                          // thread-safe "compute a new state from the old" helper
import kotlinx.coroutines.launch                               // starts a coroutine (for the fake suspend repo calls)

// ===========================================================================
// STATE  —  the single IMMUTABLE snapshot the whole screen renders from.
// ===========================================================================

/**
 * NotesUiState — one immutable object describing EVERYTHING the screen needs to
 * draw at a single moment in time.
 *
 * Every property is a `val`. The state is never edited in place; the ViewModel
 * always produces a NEW instance via [copy]. Bundling all UI fields into one
 * object (instead of many separate flows) means the screen always renders a
 * CONSISTENT snapshot — there is no window where some fields are updated and
 * others are stale.
 *
 * @param notes        the notes to display (state flows DOWN to the list).
 * @param isLoading    true while the fake repository is "loading" — drives a spinner.
 * @param newNoteTitle the in-progress text in the "add note" field. Living HERE
 *                     (not in the composable) is exactly why it survives rotation.
 */
data class NotesUiState(
    val notes: List<Note> = emptyList(),                       // val -> immutable; replaced wholesale on change
    val isLoading: Boolean = false,                            // val -> immutable loading flag
    val newNoteTitle: String = "",                            // val -> immutable draft text (survives rotation)
)

// ===========================================================================
// EVENTS  —  the FIXED vocabulary of things the UI may ask the ViewModel to do.
// ===========================================================================

/**
 * NotesEvent — a sealed interface enumerating every user intention.
 *
 * "Event up, state down": the UI never mutates state; instead it emits one of
 * these events through [NotesViewModel.onEvent]. Because the interface is
 * `sealed`, the compiler knows the complete set of subtypes, so the `when` in
 * onEvent can be exhaustive WITHOUT an `else` — add a new event and the compiler
 * forces you to handle it.
 */
sealed interface NotesEvent {
    /** The user edited the "new note" text field. Carries the latest text. */
    data class NewTitleChanged(val text: String) : NotesEvent

    /** The user pressed "Add" — commit the current [NotesUiState.newNoteTitle]. */
    data object AddNote : NotesEvent

    /** The user tapped a note's checkbox. Carries which note (by id). */
    data class ToggleDone(val id: Int) : NotesEvent

    /** The user tapped a note's delete button. Carries which note (by id). */
    data class DeleteNote(val id: Int) : NotesEvent
}

// ===========================================================================
// VIEWMODEL  —  owns the state, runs the (fake) async work, applies the events.
// ===========================================================================

/**
 * NotesViewModel — the single owner of the screen's state and the ONLY place
 * that business logic lives.
 *
 * It exposes [uiState] as a read-only [StateFlow]; the UI collects that flow and
 * sends user actions back through [onEvent]. The ViewModel is retained across
 * configuration changes, so the state in [uiState] survives rotation.
 *
 * The repository defaults to a fresh [NotesRepository], but is a constructor
 * parameter so a test could pass a fake — dependency injection in miniature.
 */
class NotesViewModel(
    private val repository: NotesRepository = NotesRepository(),
) : ViewModel() {

    // --- The writable state holder (PRIVATE) ---------------------------------
    // Only the ViewModel may write to this. It starts in a "loading" state so the
    // very first frame the UI sees is the spinner, before loadNotes() returns.
    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))

    // --- The read-only state stream (PUBLIC) ---------------------------------
    // The UI can observe and read this, but cannot write it. This one-way exposure
    // is what enforces "state flows DOWN, events flow UP".
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    // The ViewModel kicks off the initial load as soon as it is created. Because
    // viewModelScope outlives recomposition AND configuration changes, the load is
    // not restarted on every rotation — the ViewModel (and its state) is reused.
    init {
        loadNotes()
    }

    /**
     * onEvent — the SINGLE funnel through which the UI mutates state.
     *
     * Trace the UDF loop here: a [NotesEvent] arrives (step 3), we compute a NEW
     * immutable [NotesUiState] (step 4) and publish it via [_uiState] (step 5).
     * The `when` is exhaustive over the sealed [NotesEvent] — no `else` needed.
     */
    fun onEvent(event: NotesEvent) {
        when (event) {
            // Pure, synchronous state update: replace ONLY the draft text. We use
            // .copy() so every other field (notes, isLoading) is preserved untouched.
            is NotesEvent.NewTitleChanged ->
                _uiState.update { current -> current.copy(newNoteTitle = event.text) }

            // Commit the draft. Guarded so blank titles are ignored.
            is NotesEvent.AddNote -> addNote()

            // Async toggle: ask the fake repo, then re-load to reflect the change.
            is NotesEvent.ToggleDone -> toggleDone(event.id)

            // Async delete: ask the fake repo, then re-load.
            is NotesEvent.DeleteNote -> deleteNote(event.id)
        }
    }

    // --- Private helpers: each performs the (fake) async work in a coroutine ---

    /**
     * Load notes from the (fake) repository.
     *
     * Pattern to notice: flip isLoading ON (new state), await the suspend call,
     * then emit a new state carrying the result with isLoading OFF. Each step is a
     * fresh immutable [NotesUiState] produced with [copy].
     */
    private fun loadNotes() {
        viewModelScope.launch {                                // coroutine tied to the ViewModel's lifetime
            _uiState.update { it.copy(isLoading = true) }      // NEW state: show the spinner
            val loaded = repository.loadNotes()                // suspends; does NOT block the UI thread
            _uiState.update { it.copy(notes = loaded, isLoading = false) } // NEW state: data in, spinner off
        }
    }

    /**
     * Commit the current draft as a new note, then refresh the list.
     */
    private fun addNote() {
        val title = _uiState.value.newNoteTitle.trim()         // read the latest draft from the current state
        if (title.isBlank()) return                            // ignore empty submissions (no state change)
        viewModelScope.launch {
            repository.addNote(title)                          // suspend: persist to the fake store
            // Clear the draft immediately (a new immutable state)...
            _uiState.update { it.copy(newNoteTitle = "") }
            // ...then reload so the freshly-added note appears in the list.
            val loaded = repository.loadNotes()
            _uiState.update { it.copy(notes = loaded) }
        }
    }

    /**
     * Toggle one note's done flag in the (fake) repository, then refresh.
     */
    private fun toggleDone(id: Int) {
        viewModelScope.launch {
            repository.toggleDone(id)                          // suspend: flip + persist
            val loaded = repository.loadNotes()               // re-read the source of truth
            _uiState.update { it.copy(notes = loaded) }        // NEW state with the updated list
        }
    }

    /**
     * Delete one note in the (fake) repository, then refresh.
     */
    private fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNote(id)                          // suspend: delete + persist
            val loaded = repository.loadNotes()
            _uiState.update { it.copy(notes = loaded) }
        }
    }
}
