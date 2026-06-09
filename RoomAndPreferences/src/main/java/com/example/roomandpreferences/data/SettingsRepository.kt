// =============================================================================
// SettingsRepository.kt  —  DATASTORE PREFERENCES (simple settings persistence)
//
// CONCEPT: LOCAL PERSISTENCE — the OTHER half of this project.
// Use Room for STRUCTURED data (the notes table). Use DataStore Preferences for
// SIMPLE key/value SETTINGS — here a dark-theme boolean and a sort-order enum.
//
// >>> DataStore REPLACES SharedPreferences. <<<
// SharedPreferences is the OLD API: it exposed synchronous getters that could do
// disk I/O on the main thread (ANRs), used error-prone callbacks for changes, and
// had no coroutine support. DataStore is the MODERN replacement: it is fully
// asynchronous, reads via Flow (reactive — emits on every change), and writes via
// a transactional `edit { }` suspend block. Prefer DataStore in all new code.
//
// WHAT TO INSPECT HERE:
//   • preferencesDataStore(...)  — creates ONE DataStore for the whole app.
//   • Keys (booleanPreferencesKey / stringPreferencesKey) — typed setting names.
//   • darkTheme: Flow<Boolean> & sortOrder: Flow<SortOrder> — REACTIVE reads.
//   • setDarkTheme/setSortOrder — suspend writes using `edit { }` (transactional).
// =============================================================================
package com.example.roomandpreferences.data

import android.content.Context                                   // owns the DataStore extension property
import androidx.datastore.core.DataStore                         // the generic async data holder type
import androidx.datastore.preferences.core.Preferences           // the key->value map DataStore stores
import androidx.datastore.preferences.core.booleanPreferencesKey // typed key for a Boolean setting
import androidx.datastore.preferences.core.edit                  // suspend, TRANSACTIONAL write block
import androidx.datastore.preferences.core.stringPreferencesKey  // typed key for a String setting
import androidx.datastore.preferences.preferencesDataStore       // delegate that creates the DataStore once
import kotlinx.coroutines.flow.Flow                              // reactive STREAM of setting values
import kotlinx.coroutines.flow.map                               // transforms each emitted Preferences snapshot

// ---------------------------------------------------------------------------
// DATA  (settings persistence)
// ---------------------------------------------------------------------------

/**
 * How the notes list should be ordered. Persisted by NAME (enum.name) as a String
 * in DataStore — never persist `ordinal`, because reordering the enum would silently
 * change the stored meaning.
 */
enum class SortOrder {
    NEWEST_FIRST,   // order by id DESC (insertion order, newest on top)
    TITLE_ASC,      // order alphabetically by title
}

// A single, app-wide DataStore named "settings", created lazily on first access.
// Declared as a Context extension so any Context can reach `context.settingsDataStore`.
// Calling preferencesDataStore more than once for the same name throws — so it lives
// here at file scope, created exactly once.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository over DataStore Preferences for the app's simple settings.
 *
 * Exposes each setting as a [Flow] (reactive read) and provides `suspend` setters
 * that write through DataStore's transactional [edit] block. This class is the
 * DataStore counterpart to [NoteDao] (which is the Room read/write surface).
 *
 * @param context any Context; we use applicationContext internally to avoid leaks.
 */
class SettingsRepository(context: Context) {

    // Hold the application-scoped DataStore (not the Activity) to avoid leaking it.
    private val dataStore = context.applicationContext.settingsDataStore

    // --- TYPED KEYS (the "names" of each setting) ---------------------------
    private companion object {
        // Dark-theme on/off. booleanPreferencesKey makes the name + type explicit.
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        // Sort order, stored as the enum's NAME string (stable across reorders).
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
    }

    /**
     * The dark-theme setting as a reactive stream. Emits a fresh value every time
     * the stored preference changes. Defaults to `false` (light) when never set.
     */
    val darkTheme: Flow<Boolean> = dataStore.data.map { prefs ->   // <-- REACTIVE read from DataStore
        prefs[KEY_DARK_THEME] ?: false                             // default when the key is absent
    }

    /**
     * The sort-order setting as a reactive stream. Reads back the stored enum NAME
     * and maps it to a [SortOrder], defaulting to NEWEST_FIRST.
     */
    val sortOrder: Flow<SortOrder> = dataStore.data.map { prefs ->
        // valueOf throws on unknown text, so guard with runCatching -> fall back to default.
        runCatching { SortOrder.valueOf(prefs[KEY_SORT_ORDER] ?: SortOrder.NEWEST_FIRST.name) }
            .getOrDefault(SortOrder.NEWEST_FIRST)
    }

    /**
     * Persist the dark-theme flag. `edit { }` is a SUSPEND, TRANSACTIONAL write:
     * the lambda receives a mutable copy of the preferences, and DataStore atomically
     * commits the result to disk. This is the modern replacement for
     * SharedPreferences.edit().putBoolean(...).apply().
     */
    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->                                  // <-- suspend + transactional write
            prefs[KEY_DARK_THEME] = enabled
        }
    }

    /** Persist the sort order by storing the enum's stable NAME string. */
    suspend fun setSortOrder(order: SortOrder) {
        dataStore.edit { prefs ->
            prefs[KEY_SORT_ORDER] = order.name                     // store NAME, never ordinal
        }
    }
}
