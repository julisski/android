// =============================================================================
// NoteDao.kt  —  THE DATA ACCESS OBJECT (the "queries" for the notes table)
//
// CONCEPT: LOCAL PERSISTENCE with Room — the READ/WRITE surface.
// A DAO is an INTERFACE you describe; Room generates the implementation (NoteDao_Impl)
// during the build (via KSP). You never write `new NoteDao()` — Room hands you the
// generated class from the database object (see NoteDatabase.kt).
//
// WHAT TO INSPECT HERE (the heart of the Room demo):
//   • observeNotes(): Flow<List<Note>>  — a REACTIVE query. Room re-emits a fresh
//        list EVERY TIME the "notes" table changes. The UI just collects this Flow
//        and updates automatically — no manual refresh after insert/delete.
//   • suspend insert/update/delete       — these touch disk, so they are `suspend`
//        functions and must run off the main thread (the ViewModel uses coroutines).
// =============================================================================
package com.example.roomandpreferences.data

import androidx.room.Dao       // marks an interface as a Room Data Access Object
import androidx.room.Delete    // generates a DELETE-by-primary-key statement
import androidx.room.Insert    // generates an INSERT statement
import androidx.room.Query     // lets you attach a custom SQL string to a function
import androidx.room.Update    // generates an UPDATE-by-primary-key statement
import kotlinx.coroutines.flow.Flow  // a cold async STREAM; Room emits a new list on every table change

// ---------------------------------------------------------------------------
// DATA  (queries)
// ---------------------------------------------------------------------------

/**
 * Data Access Object for [Note] rows.
 *
 * This is only an INTERFACE — Room generates the concrete `NoteDao_Impl` at build
 * time from the annotations below. Inspect that generated file via
 * Build > Generated Sources, or just trust that each annotated method maps to one
 * SQLite operation.
 */
@Dao
interface NoteDao {

    /**
     * Observe ALL notes as a reactive stream, newest first.
     *
     * THE KEY ROOM IDEA: a query that returns [Flow] is "live". Room watches the
     * "notes" table; whenever a row is inserted/updated/deleted, it re-runs this
     * SELECT and pushes a brand-new List down the Flow. Collectors (our ViewModel
     * / UI) update with zero extra work.
     */
    @Query("SELECT * FROM notes ORDER BY id DESC")          // <-- reactive read; re-emits on every table change
    fun observeNotes(): Flow<List<Note>>

    /**
     * Observe all notes ordered ALPHABETICALLY by title (case-insensitive).
     * Used to demonstrate the DataStore "sort order" setting changing which Flow
     * the ViewModel collects.
     */
    @Query("SELECT * FROM notes ORDER BY title COLLATE NOCASE ASC")
    fun observeNotesByTitle(): Flow<List<Note>>

    /**
     * Insert one note. `suspend` because writing to disk must not block the UI
     * thread; the ViewModel calls this from a coroutine.
     */
    @Insert
    suspend fun insert(note: Note)                          // <-- disk write => suspend

    /** Update an existing note (matched by its @PrimaryKey id). */
    @Update
    suspend fun update(note: Note)                          // <-- disk write => suspend

    /** Delete a note (matched by its @PrimaryKey id). */
    @Delete
    suspend fun delete(note: Note)                          // <-- disk write => suspend
}
