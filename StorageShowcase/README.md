# Storage Showcase

A single Jetpack Compose teaching app that demonstrates **every common on-device storage
technique** in one place — the runnable capstone behind the interactive
[`Playground/storage-playground.html`](../Playground/storage-playground.html) lab and a
broader sibling to [`RoomAndPreferences`](../RoomAndPreferences).

Four tabs, one storage mechanism each — everything you change survives an app restart:

| Tab | Mechanism | What it shows |
|---|---|---|
| **Notes** | **Room** (SQLite) | `@Entity` (Note + Category), a `@ForeignKey` + `@Index` relation, `@Relation`, a `@TypeConverter` (List→JSON, enum→name), reactive `Flow<List<Note>>` queries, and a real `Migration(1,2)`. |
| **Settings** | **Preferences DataStore** | Dark-theme flag + notes sort order — reactive `Flow` reads, transactional `edit { }` writes (replaces SharedPreferences). |
| **Profile** | **Typed DataStore** | One `@Serializable UserProfile` object stored as JSON via a custom `Serializer<T>` + `updateData { }` — type-safe, not loose keys. |
| **Files** | **Internal file storage** | A scratch note in `filesDir` (persists, gone on uninstall) and a `cacheDir` blob (evictable); raw file I/O dispatched to `Dispatchers.IO`. |

Everything is wired through one `StorageViewModel`: persistence `Flow`s become UI `StateFlow`s
via `stateIn(...)`, the sort setting swaps the Room query with `flatMapLatest`, and the UI reads
it all with `collectAsStateWithLifecycle()`. The source carries heavy, line-by-line teaching
comments, like the rest of this repo.

## Key files

```
src/main/java/com/example/storageshowcase/
  data/
    Models.kt            @Entity Note + Category, @Relation CategoryWithNotes, Priority enum
    Converters.kt        @TypeConverter: List<String>↔JSON, Priority↔name
    NoteDao.kt           @Dao — reactive Flow reads + suspend writes + @Transaction relation
    StorageDatabase.kt   @Database holder, singleton, Migration(1,2)
    SettingsRepository.kt  Preferences DataStore (dark theme + sort order)
    ProfileRepository.kt   Typed DataStore (UserProfile as JSON) + Serializer
    FileStore.kt           filesDir / cacheDir file I/O
  StorageViewModel.kt    ties all four layers to the UI as StateFlows
  MainActivity.kt        tabbed Compose UI (one tab per mechanism)
src/test/java/.../ConvertersTest.kt   JVM unit test for the type converters
```

## Tech stack

Jetpack Compose · Material 3 · Room `2.7.2` via **KSP** · DataStore (Preferences + Typed)
`1.1.7` · kotlinx.serialization `1.7.3` · Kotlin `2.2.10` · AGP `9.2.1` · `compileSdk 37` /
`minSdk 24`.

## Run

```bash
./gradlew :compileDebugKotlin     # type-check + run Room's KSP processor
./gradlew :testDebugUnitTest      # run the converter unit tests
./gradlew installDebug            # build & install on a running device/emulator
```

Inspect the live database with **Android Studio ▸ View ▸ Tool Windows ▸ App Inspection ▸
Database Inspector** (open `storage.db`) and watch rows appear as you add notes.
