# Room And Preferences

A small Jetpack Compose teaching app that demonstrates **local persistence** on
Android using the two modern, complementary tools:

- **Room** for **structured data** — a list of notes stored in a real SQLite table.
- **DataStore Preferences** for **simple settings** — a dark-theme boolean and a
  sort-order enum.

Everything you do in the app (adding notes, checking them off, flipping the
dark-theme switch, changing the sort order) is written to disk and **survives an
app restart**.

## Learning goal

Learn when and how to persist data locally on Android:

- Use **Room** when you have **structured / relational data** (rows with fields,
  queries, ordering) — here, the notes. Room maps an annotated Kotlin class to a
  SQLite table and generates all the SQL for you (via the KSP annotation processor
  at build time).
- Use **DataStore Preferences** when you have **simple key/value settings** — here,
  a dark-theme `Boolean` and a `SortOrder` enum. **DataStore is the modern
  replacement for `SharedPreferences`**: it is fully asynchronous, reads values as
  reactive `Flow`s, and writes through a transactional `edit { }` block, avoiding
  the main-thread disk I/O and clumsy change callbacks of the old
  `SharedPreferences` API. This project deliberately does **not** use
  `SharedPreferences`.
- See how both persistence layers expose **reactive `Flow`s** that feed a
  `ViewModel`, which surfaces them as Compose state so the UI updates automatically.

## Key files

- `src/main/java/com/example/roomandpreferences/data/Note.kt` — the Room
  **`@Entity`**: one annotated data class that becomes the `notes` SQLite table.
- `src/main/java/com/example/roomandpreferences/data/NoteDao.kt` — the Room
  **`@Dao`**: the read/write queries, including the reactive
  `observeNotes(): Flow<List<Note>>` and `suspend` insert/update/delete.
- `src/main/java/com/example/roomandpreferences/data/NoteDatabase.kt` — the
  **`@Database`**: ties the entity + DAO together and exposes a thread-safe
  singleton built with `Room.databaseBuilder`.
- `src/main/java/com/example/roomandpreferences/data/SettingsRepository.kt` — the
  **DataStore** layer: `darkTheme: Flow<Boolean>` and `sortOrder: Flow<SortOrder>`
  reads plus `suspend` setters using `edit { }`. (Comment in the file explains that
  this replaces `SharedPreferences`.)
- `src/main/java/com/example/roomandpreferences/NotesViewModel.kt` — the
  **ViewModel**: combines the Room and DataStore `Flow`s into `StateFlow`s and
  exposes add/delete/toggle actions.
- `src/main/java/com/example/roomandpreferences/MainActivity.kt` — the **UI**: a
  notes list, an add-note form, delete buttons, and the dark-theme `Switch` wired
  to DataStore. Includes a stateless `NotesScreen` + `@Preview`s.
- `build.gradle.kts` / `gradle/libs.versions.toml` — the dependency + plugin wiring
  (Room, KSP, DataStore, lifecycle-compose).

## What to inspect

- **The DAO `Flow` (`NoteDao.kt`)** — `observeNotes()` returns
  `Flow<List<Note>>`. Room re-runs the query and re-emits a new list **every time
  the `notes` table changes**, so the UI refreshes with no manual reload after an
  insert or delete.
- **Room + KSP wiring (`build.gradle.kts`)** — the `com.google.devtools.ksp` plugin
  is applied, and the Room compiler is added with **`ksp(libs.androidx.room.compiler)`**
  (not `implementation`). KSP is what generates `NoteDatabase_Impl` / `NoteDao_Impl`
  during `assembleDebug`. The KSP version (`2.2.10-2.0.2`) is pinned to the Kotlin
  version (`2.2.10`) — a mismatch breaks the build.
- **DataStore `edit { }` and `Flow` (`SettingsRepository.kt`)** — settings are read
  as `Flow`s (`dataStore.data.map { ... }`) and written inside a `suspend`,
  transactional `dataStore.edit { prefs -> ... }` block. Note the comment that this
  **replaces `SharedPreferences`**.
- **The `flatMapLatest` switch (`NotesViewModel.kt`)** — changing the DataStore
  sort-order setting switches **which** Room query `Flow` the ViewModel collects,
  showing both persistence layers cooperating reactively.
- **Inspect the database live** — run the app, then in Android Studio open
  **View > Tool Windows > App Inspection > Database Inspector**, select `notes.db`,
  and watch rows appear/disappear as you add and delete notes. You can also confirm
  the DataStore-backed dark-theme choice persists by toggling it, killing the app,
  and relaunching.

## Run it

From the project root:

```bash
./gradlew assembleDebug      # build the debug APK (runs KSP + Room codegen)
./gradlew testDebugUnitTest  # run the example unit test
```

Then install on a device/emulator from Android Studio (Run ▶) or with
`./gradlew installDebug`. Add a few notes, toggle dark mode, then fully close and
reopen the app — your data and setting are still there.
