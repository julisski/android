# Mvvm State

A tiny, heavily-commented Jetpack Compose teaching app built around a **notes**
screen. Persistence and networking are intentionally **fake** (an in-memory list
with a simulated delay) so nothing distracts from the architecture lesson.

## Learning goal

Teach **MVVM with a ViewModel, a `StateFlow`, an immutable `UiState`, UI events,
and unidirectional data flow (UDF)**.

The one rule the whole app demonstrates: **state flows DOWN, events flow UP.**

```
UI renders state  ->  user action  ->  onEvent(event)  ->  ViewModel computes a
NEW immutable state  ->  StateFlow emits  ->  UI recomposes  ->  (repeat)
```

You should come away understanding:

- Why the UI never mutates state directly — it only sends **events** to a single
  `onEvent(...)` funnel.
- Why `NotesUiState` is **immutable** (all `val`s) and only ever changes via
  `.copy(...)`.
- Why the `ViewModel`'s state **survives screen rotation** (the ViewModel is
  retained across configuration changes; the state lives in it, not in the
  composable).

## Key files

- `src/main/java/com/example/mvvmstate/NotesRepository.kt` — the **DATA** layer:
  the immutable `Note` model and a FAKE in-memory repository whose `suspend`
  functions add a small `delay()` to mimic disk/network latency.
- `src/main/java/com/example/mvvmstate/NotesViewModel.kt` — the **heart of the
  lesson**: the immutable `NotesUiState`, the sealed `NotesEvent` interface, and
  `NotesViewModel` exposing `uiState: StateFlow<NotesUiState>` plus the single
  `onEvent(event)` funnel.
- `src/main/java/com/example/mvvmstate/MainActivity.kt` — the **UI**: a stateful
  `NotesScreen` (owns the ViewModel, collects the flow) and a stateless
  `NotesScreenContent` (pure function of state + an `onEvent` lambda), plus the
  `@Preview`s.
- `src/test/java/com/example/mvvmstate/ExampleUnitTest.kt` — small tests showing
  that `.copy(...)` produces a new value without mutating the original.

## What to inspect

- **`NotesViewModel.onEvent(...)`** — the ONLY place state changes. Trace each
  `when` branch to a `_uiState.update { it.copy(...) }` call and match it to the
  six numbered steps of the UDF loop in the file header.
- **`_uiState` vs `uiState`** in the ViewModel — a private `MutableStateFlow`
  (writable by the VM) exposed publicly as a read-only `StateFlow`. The UI can
  read state but can never write it.
- **`NotesUiState`** — every property is a `val`. Notice nothing is ever edited
  in place; the ViewModel always builds a NEW state with `.copy(...)`.
- **`collectAsStateWithLifecycle()`** in `NotesScreen` — turns the `StateFlow`
  into Compose `State` so the screen recomposes on every emission (UDF step 6),
  and stops collecting while the screen isn't visible.
- **`NotesScreenContent`** — it is STATELESS (takes a `NotesUiState` + an
  `onEvent` sink). That is exactly why the `@Preview`s can hand it fabricated
  state and never need to construct a ViewModel.
- **`isLoading`** — one boolean in the immutable state decides spinner-vs-list.
  There is no separate mutable "loading" variable hiding in the UI.

### Try the rotation experiment

Run the app, type some text into the **New note** field (do NOT tap Add), then
rotate the device/emulator. The draft text and the list are still there —
because that state lives in the retained `NotesViewModel`, not in the composable.

## Run it

Open the project in Android Studio and Run, or from the project root:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run the example unit tests
```
