// =============================================================================
// Note.kt  —  THE ROOM ENTITY (one database table, described as a Kotlin class)
//
// CONCEPT: LOCAL PERSISTENCE with Room.
// Room is Android's official SQLite object-mapping library. Instead of writing
// raw SQL "CREATE TABLE" statements, you describe a TABLE as an annotated Kotlin
// data class. Room reads these annotations AT COMPILE TIME (via KSP — see
// build.gradle.kts) and generates all the SQLite plumbing for you.
//
// WHAT TO INSPECT HERE:
//   • @Entity            — marks this class as a database table.
//   • @PrimaryKey        — the unique row id; autoGenerate=true lets SQLite assign it.
//   • The plain fields   — each becomes a COLUMN in the "notes" table.
// Compare this file with NoteDao.kt (the queries) and NoteDatabase.kt (the wiring).
// =============================================================================
package com.example.roomandpreferences.data

import androidx.room.Entity      // marks a class as a Room table (one row == one object)
import androidx.room.PrimaryKey  // marks the field that uniquely identifies each row

// ---------------------------------------------------------------------------
// DATA
// ---------------------------------------------------------------------------

/**
 * A single persisted note — ONE ROW in the SQLite "notes" table.
 *
 * Room turns this annotated data class into a real table at compile time:
 *   each constructor property below becomes a COLUMN, and each Note instance
 *   becomes a ROW. Because it is annotated [@Entity], Room knows how to read it
 *   out of and write it into SQLite without any hand-written SQL.
 *
 * @property id    primary key. Defaults to 0; with autoGenerate=true, passing 0
 *                 on insert tells SQLite "assign the next id for me".
 * @property title the note's short headline (a TEXT column).
 * @property body  the note's longer text (a TEXT column).
 * @property done  whether the note is checked off (stored as INTEGER 0/1).
 */
@Entity(tableName = "notes")                 // <-- THE concept: this class IS a table named "notes"
data class Note(
    @PrimaryKey(autoGenerate = true)         // <-- unique row id; SQLite auto-increments it
    val id: Long = 0,                        // default 0 => "let Room/SQLite pick the id on insert"
    val title: String,                       // becomes a TEXT column "title"
    val body: String,                        // becomes a TEXT column "body"
    val done: Boolean = false,               // becomes an INTEGER column "done" (0 = false, 1 = true)
)
