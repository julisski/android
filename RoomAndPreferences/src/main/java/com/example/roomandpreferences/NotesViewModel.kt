// =============================================================================
// NotesViewModel.kt  —  STATE HOLDER bridging persistence (Room + DataStore) to UI
//
// CONCEPT: LOCAL PERSISTENCE surfaced as Compose STATE.
// The ViewModel is where the two persistence Flows (Room's notes + DataStore's
// settings) become observable UI state, and where user actions become suspend
// writes launched on viewModelScope. It survives configuration changes (rotation),
// so the screen never re-queries on every recomposition.
//
// WHAT TO INSPECT HERE:
//   • notes / darkTheme / sortOrder StateFlows — derived from the persistence Flows
//        via stateIn(viewModelScope, ...). The UI collects these.
//   • flatMapLatest on sortOrder — switching the DataStore setting SWITCHES which
//        Room query Flow we observe. Persistence + reactivity working together.
//   • addNote/deleteNote/setDarkTheme — viewModelScope.launch { suspend write }.
//   • Factory — constructs the ViewModel with the DAO + repository (no DI library).
// =============================================================================
package com.example.roomandpreferences

import android.app.Application                                       // app Context source for building DB + DataStore
import androidx.lifecycle.AndroidViewModel                          // ViewModel that holds an Application reference
import androidx.lifecycle.ViewModel                                 // base class (referenced by the Factory type)
import androidx.lifecycle.ViewModelProvider                         // factory interface to construct ViewModels
import androidx.lifecycle.viewModelScope                            // CoroutineScope tied to the ViewModel's lifetime
import com.example.roomandpreferences.data.Note                     // the Room entity (a note row)
import com.example.roomandpreferences.data.NoteDatabase             // Room database singleton (provides the DAO)
import com.example.roomandpreferences.data.SettingsRepository       // DataStore-backed settings
import com.example.roomandpreferences.data.SortOrder                // sort-order enum persisted in DataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi                // flatMapLatest is still marked experimental
import kotlinx.coroutines.flow.SharingStarted                      // controls WHEN the StateFlow is kept hot
import kotlinx.coroutines.flow.StateFlow                           // observable, always-has-a-value stream for UI
import kotlinx.coroutines.flow.flatMapLatest                       // switch to a new inner Flow when the outer changes
import kotlinx.coroutines.flow.stateIn                             // converts a cold Flow into a hot StateFlow
import kotlinx.coroutines.launch                                   // starts a coroutine for the suspend writes

// ---------------------------------------------------------------------------
// STATE
// ---------------------------------------------------------------------------

/**
 * ViewModel for the notes screen. Owns the persistence wiring and exposes
 * everything the UI needs as [StateFlow]s.
 *
 * Extends [AndroidViewModel] so it can obtain an [Application] Context to build the
 * Room database and the DataStore-backed [SettingsRepository] — without leaking an
 * Activity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(app: Application) : AndroidViewModel(app) {

    // The Room DAO (read/write surface for the "notes" table).
    private val noteDao = NoteDatabase.getInstance(app).noteDao()

    // The DataStore-backed settings repository (dark theme + sort order).
    private val settings = SettingsRepository(app)

    /**
     * The dark-theme setting as UI state. We collect DataStore's Flow and cache the
     * latest value in a StateFlow so Compose always has a value to render.
     */
    val darkTheme: StateFlow<Boolean> = settings.darkTheme
        .stateIn(
            scope = viewModelScope,                                 // lives as long as the ViewModel
            started = SharingStarted.WhileSubscribed(5_000),       // stay hot 5s after last collector
            initialValue = false,                                  // render light until the first emission
        )

    /** The current sort order as UI state (drives both the toggle and the query). */
    val sortOrder: StateFlow<SortOrder> = settings.sortOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortOrder.NEWEST_FIRST)

    /**
     * The persisted notes as UI state.
     *
     * KEY WIRING: we observe the DataStore sort-order Flow and, via [flatMapLatest],
     * SWITCH to whichever Room query Flow matches it. Change the setting -> a new
     * Room Flow is collected -> the list re-sorts. Both persistence layers are
     * reactive, and they compose cleanly.
     */
    val notes: StateFlow<List<Note>> = settings.sortOrder
        .flatMapLatest { order ->                                  // <-- setting change swaps the Room query
            when (order) {
                SortOrder.NEWEST_FIRST -> noteDao.observeNotes()       // ORDER BY id DESC
                SortOrder.TITLE_ASC    -> noteDao.observeNotesByTitle() // ORDER BY title ASC
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- ACTIONS (user intents -> suspend persistence writes) ----------------

    /** Add a note. Launches on viewModelScope so the suspend DB insert runs off the UI thread. */
    fun addNote(title: String, body: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return                            // ignore empty submissions
        viewModelScope.launch {                                     // coroutine -> safe to call suspend insert
            noteDao.insert(Note(title = cleanTitle, body = body.trim()))
        }
    }

    /** Delete a note (the row's @PrimaryKey id is used to find it). */
    fun deleteNote(note: Note) {
        viewModelScope.launch { noteDao.delete(note) }
    }

    /** Toggle a note's done flag and persist it (UPDATE by primary key). */
    fun toggleDone(note: Note) {
        viewModelScope.launch { noteDao.update(note.copy(done = !note.done)) }
    }

    /** Persist the dark-theme setting via DataStore's transactional edit{}. */
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settings.setDarkTheme(enabled) }
    }

    /** Persist the sort-order setting via DataStore. */
    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }

    /**
     * Factory that constructs [NotesViewModel] with an [Application].
     *
     * AndroidViewModel needs the Application passed in, so we supply a tiny factory
     * rather than pulling in a DI library for this teaching sample.
     */
    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NotesViewModel(app) as T
            }
    }
}
