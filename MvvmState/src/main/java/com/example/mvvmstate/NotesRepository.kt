// =============================================================================
// NotesRepository.kt
//
// CONCEPT FOCUS: the DATA layer for the MVVM lesson.
//
// In MVVM the ViewModel never talks to a database or a network directly — it
// asks a REPOSITORY. The repository hides "where the data comes from" behind a
// few suspend functions. Here that source is a FAKE, in-memory list with a tiny
// artificial delay, so the architecture lesson stays focused: there is NO real
// Room database and NO Retrofit network call to distract from MVVM + StateFlow.
//
// WHAT TO INSPECT (student):
//   • Every function is `suspend` — it can pause without blocking the UI thread.
//     The ViewModel will call these from inside viewModelScope (a coroutine).
//   • `delay(...)` simulates I/O latency; it is the reason NotesUiState has an
//     `isLoading` flag the UI can render a spinner for.
//   • The repository returns plain immutable data (`Note`, `List<Note>`); it does
//     NOT expose mutable state. The single source of truth lives in the ViewModel.
// =============================================================================
package com.example.mvvmstate                                  // same package as the rest of the app

import kotlinx.coroutines.delay                                // suspends a coroutine to FAKE network/disk latency

// ===========================================================================
// DATA MODEL
// ===========================================================================

/**
 * A single note — an IMMUTABLE value object.
 *
 * Every property is a `val`, so a Note can never be mutated in place. To "change"
 * a note we build a new copy with [copy]. Immutable models are the foundation of
 * unidirectional data flow: state only ever changes by producing a NEW value.
 *
 * @param id    stable unique identifier (used by ToggleDone / DeleteNote events).
 * @param title the note's text.
 * @param done  whether the note has been checked off.
 */
data class Note(
    val id: Int,                                               // immutable identity
    val title: String,                                        // immutable content
    val done: Boolean = false,                                // immutable status; defaults to "not done"
)

// ===========================================================================
// FAKE REPOSITORY
// ===========================================================================

/**
 * NotesRepository — a FAKE in-memory data source for the lesson.
 *
 * Think of this as the seam where a real app would plug in Room or Retrofit. By
 * keeping it fake we get realistic asynchronous behaviour (suspend + delay)
 * without any real persistence/network, so the focus stays on MVVM + StateFlow.
 *
 * IMPORTANT: this class holds NO observable state. It just answers questions
 * ("load the notes") and performs actions ("save this note"). The authoritative
 * list of notes is owned by [NotesViewModel] — this keeps a SINGLE source of truth.
 */
class NotesRepository {

    // A private, mutable seed list. It is an implementation detail of the fake
    // data source and is never exposed directly to the UI or the ViewModel.
    private val notes = mutableListOf(
        Note(id = 1, title = "Read about ViewModel", done = true),
        Note(id = 2, title = "Understand StateFlow", done = false),
        Note(id = 3, title = "Draw the UDF loop", done = false),
    )

    /**
     * Pretend to load notes from disk/network.
     *
     * `suspend` + [delay] models latency: the caller (the ViewModel) can flip its
     * UiState to `isLoading = true`, await this function, then flip back. We return
     * a defensive copy (`toList()`) so callers can never mutate our internal list.
     */
    suspend fun loadNotes(): List<Note> {
        delay(600)                                            // FAKE I/O latency -> justifies the isLoading flag
        return notes.toList()                                // hand back an immutable snapshot
    }

    /**
     * Pretend to persist a brand-new note, returning the saved note (with its id).
     *
     * A real repo would INSERT a row and let the DB assign the id; here we compute
     * the next id ourselves. The point for the lesson: this is async work the
     * ViewModel performs OFF the main thread inside a coroutine.
     */
    suspend fun addNote(title: String): Note {
        delay(300)                                            // FAKE write latency
        val nextId = (notes.maxOfOrNull { it.id } ?: 0) + 1  // compute a fresh unique id
        val created = Note(id = nextId, title = title)        // build the new immutable Note
        notes.add(created)                                   // store it in the fake backing list
        return created                                        // return it so the ViewModel can add it to UiState
    }

    /**
     * Pretend to flip a note's `done` flag and persist the change.
     *
     * Note how we REPLACE the element with a `copy(...)` rather than mutating it:
     * even inside the data layer we treat [Note] as immutable.
     */
    suspend fun toggleDone(id: Int) {
        delay(150)                                            // FAKE write latency
        val index = notes.indexOfFirst { it.id == id }        // find the note to update
        if (index != -1) {
            val current = notes[index]
            notes[index] = current.copy(done = !current.done) // immutable update via copy()
        }
    }

    /**
     * Pretend to delete a note by id.
     */
    suspend fun deleteNote(id: Int) {
        delay(150)                                            // FAKE write latency
        notes.removeAll { it.id == id }                       // drop the matching note from the fake store
    }
}
