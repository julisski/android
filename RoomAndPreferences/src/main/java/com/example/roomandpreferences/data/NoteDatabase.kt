// =============================================================================
// NoteDatabase.kt  —  THE ROOM DATABASE (ties the Entity + DAO into a real DB file)
//
// CONCEPT: LOCAL PERSISTENCE with Room — the database holder + singleton.
// This abstract class is the single connection point to the underlying SQLite
// file on disk. Room generates the concrete NoteDatabase_Impl at build time (KSP),
// which is why the class and noteDao() are `abstract` — YOU never implement them.
//
// WHAT TO INSPECT HERE:
//   • @Database(entities=[Note::class], version=1, exportSchema=false)
//        — lists every table (entity) and the schema version. exportSchema=false
//          avoids needing a schema-JSON output directory for this teaching sample.
//   • abstract fun noteDao()  — Room implements this to return the generated DAO.
//   • getInstance()           — a thread-safe SINGLETON built with Room.databaseBuilder.
//        Creating more than one database instance for the same file is wasteful and
//        can corrupt state, so we cache exactly one.
// =============================================================================
package com.example.roomandpreferences.data

import android.content.Context              // needed by Room.databaseBuilder to locate app storage
import androidx.room.Database               // marks this class as the Room database + lists its tables
import androidx.room.Room                   // factory used to build the database instance
import androidx.room.RoomDatabase           // base class every Room database extends

// ---------------------------------------------------------------------------
// DATA  (database holder)
// ---------------------------------------------------------------------------

/**
 * The app's Room database. Holds the single "notes" table and exposes its DAO.
 *
 * It is `abstract`: Room GENERATES the real subclass (NoteDatabase_Impl) at compile
 * time. The @Database annotation tells Room which entities (tables) belong to this
 * database and the schema version (bump it + add a Migration when the schema changes).
 */
@Database(
    entities = [Note::class],               // <-- every table in this DB; here just Note
    version = 1,                            // schema version; increment when columns change
    exportSchema = false,                   // <-- skip writing schema JSON (no schema dir needed for the demo)
)
abstract class NoteDatabase : RoomDatabase() {

    /**
     * Room implements this to return the generated [NoteDao]. You call
     * `db.noteDao()` to get the object whose annotated methods run real SQL.
     */
    abstract fun noteDao(): NoteDao         // <-- Room fills this in with NoteDao_Impl

    // -----------------------------------------------------------------------
    // SINGLETON — exactly ONE database instance per process.
    // -----------------------------------------------------------------------
    companion object {
        // @Volatile: writes to INSTANCE are immediately visible to all threads,
        // so the double-checked locking below is correct under concurrency.
        @Volatile private var INSTANCE: NoteDatabase? = null

        /**
         * Return the shared [NoteDatabase], creating it on first use.
         *
         * Uses double-checked locking so two threads racing on startup can't each
         * build their own database instance for the same file.
         */
        fun getInstance(context: Context): NoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,             // applicationContext => no Activity leak
                    NoteDatabase::class.java,               // the database class to implement
                    "notes.db",                             // the SQLite FILE name on disk
                ).build().also { INSTANCE = it }            // cache the instance for next time
            }
    }
}
