# Network Parsing

A single-screen Jetpack Compose app that fetches a list of notes from a public JSON
API and renders explicit **Loading / Success / Error** states. It teaches the
end-to-end "talk to a network, parse JSON, show it safely" flow using **Retrofit** +
**kotlinx.serialization** + a **ViewModel** exposing a `StateFlow`.

## Learning goal

Learn how an Android app does **networking and JSON parsing**, and how to surface the
result as clear UI states:

- **DTO vs domain model** — keep the JSON wire shape (`NoteDto`, `@Serializable`)
  separate from the clean shape the app uses (`Note`), and convert in one mapper.
- **Retrofit + `suspend`** — describe an endpoint as an annotated interface method
  (`@GET("posts") suspend fun fetchNotes()`) and let Retrofit generate the HTTP call;
  it runs off the main thread and returns parsed objects.
- **The UiState pattern** — model the screen as a sealed `NotesUiState`
  (`Loading` / `Success(notes)` / `Error(message)`) exposed via `StateFlow`, so the UI
  is a pure function of state.
- **Error handling** — a single `try/catch` in the ViewModel turns any failure (no
  network, bad JSON, timeout) into an `Error` state instead of a crash.
- **Real vs Fake switch** — a repository interface with a network-backed `Real` impl
  and an in-memory `Fake` impl, swapped by one flag so the app and tests run offline.

## Key files

- `src/main/java/com/example/networkparsing/Note.kt` — `NoteDto` (the `@Serializable`
  JSON shape), `Note` (the domain model), and the `NoteDto.toNote()` mapper.
- `src/main/java/com/example/networkparsing/NoteApi.kt` — the Retrofit `NoteApi`
  interface and `provideNoteApi()` (baseUrl + the kotlinx.serialization converter).
- `src/main/java/com/example/networkparsing/NoteRepository.kt` — the `NoteRepository`
  interface plus `RealNoteRepository` (network) and `FakeNoteRepository` (offline),
  and `provideNoteRepository(useFake = …)` — **the switch**.
- `src/main/java/com/example/networkparsing/NotesViewModel.kt` — `NotesUiState` sealed
  interface and the ViewModel that loads notes into a `StateFlow`.
- `src/main/java/com/example/networkparsing/MainActivity.kt` — the Compose UI:
  `NotesScreen` (collects the StateFlow) and the stateless `NotesContent` that renders
  each state, plus `@Preview`s.
- `src/test/java/com/example/networkparsing/ExampleUnitTest.kt` — unit tests for the
  mapper and the offline fake repository.
- `src/main/AndroidManifest.xml` — contains the (commented) `INTERNET` permission.

## What to inspect

- **`Note.kt`**: notice `@Serializable` on `NoteDto`, that its field names match the
  JSON keys exactly (`userId/id/title/body`), and that `Note` deliberately drops
  `userId`. The `toNote()` mapper is the single network-shape → app-shape boundary.
- **`NoteApi.kt`**: `@GET("posts")`, the `suspend` function, and how Retrofit is built
  with `Json { ignoreUnknownKeys = true }` so new server fields don't break parsing.
- **`NotesViewModel.kt`**: the private `MutableStateFlow` vs the public read-only
  `StateFlow`, and the `try/catch` in `loadNotes()` that maps failures to `Error`.
- **`MainActivity.kt`**: `collectAsStateWithLifecycle()` and the exhaustive `when`
  over the sealed `NotesUiState`. The previews drive the **stateless** `NotesContent`
  with hand-made states — never a real ViewModel (a ViewModel would try to run a
  coroutine the design pane can't execute).

## Run it

By default the app runs **offline** in fake mode (`provideNoteRepository(useFake = true)`),
so it builds and runs with **no network** and the unit tests need no internet. The fake
still parses a hardcoded JSON string and adds a small delay so you can see the Loading
spinner.

To hit the **live** API (`https://jsonplaceholder.typicode.com/posts`):

1. In `NoteRepository.kt`, change the default to `provideNoteRepository(useFake = false)`.
2. In `AndroidManifest.xml`, **uncomment** the
   `<uses-permission android:name="android.permission.INTERNET" />` line.
3. Run on a device/emulator **with a network connection**.

Build from the project root:

```bash
./gradlew assembleDebug --console=plain --stacktrace
./gradlew testDebugUnitTest
```
