# Compose Lists

A single-screen Jetpack Compose teaching app for the **notes** domain. It shows
how to display and manage a *collection* of items: a scrolling list, a grid,
multi-selection, filtering, stable keys, and an empty state — all without a
single line of `RecyclerView` boilerplate.

## Learning goal

Learn how **lists** work in Compose:

- **`LazyColumn`** and **`LazyVerticalGrid`** (`GridCells.Fixed(2)`) rendering
  the *same* data, switchable with a toolbar toggle.
- **Stable item keys** via `items(list, key = { it.id })` — so Compose tracks
  each item's *identity* (not its position) across recomposition, reordering,
  and filtering. This preserves scroll position and item animations.
- **Multi-selection**: tapping a note toggles it in a hoisted `Set<Int>`;
  selected items get a highlighted background, a live selected-count, and a
  "Clear selection" action.
- **Text filtering**: a search field whose query is hoisted state; the visible
  list is *derived* from `allNotes + query`, matched case-insensitively against
  both title and body.
- **Empty state**: a dedicated composable shown when the filter matches nothing.

This project is intentionally **not** about navigation — there is one screen.

## Key files

- `src/main/java/com/example/composelists/MainActivity.kt` — the whole sample:
  the `Note` data class and seed data, the `NotesViewModel` (state), the
  stateful `NotesScreen`, the stateless `NotesContent`, the `NotesColumn` /
  `NotesGrid` lazy layouts, the `NoteCard` item, the `EmptyState`, and three
  `@Preview` functions.
- `src/main/java/com/example/composelists/ui/theme/` — `Theme.kt`, `Color.kt`,
  `Type.kt`: the Material 3 `ComposeListsTheme` that wraps the UI.
- `gradle/libs.versions.toml` and `build.gradle.kts` — dependencies. Note that
  the base project's Navigation 3 and kotlinx-serialization deps were removed
  (this project does not navigate), and `lifecycle-viewmodel-compose` was added
  for the `viewModel()` composable.

## What to inspect

- **The `key = { it.id }` argument** in *both* `NotesColumn` (`items(...)`) and
  `NotesGrid` (`gridItems(...)`). Try removing it and watch how selection
  highlight + scroll position behave when you filter the list — stable keys are
  what keep item identity correct.
- **`NotesViewModel.visibleNotes`** — a *derived* property recomputed on every
  read from `allNotes` + `query`. Nothing filtered is ever stored, so there are
  no stale duplicates to keep in sync.
- **`toggleSelected`** — it builds a *new* `Set` rather than mutating the old
  one; that immutability is what lets Compose detect the change and recompose.
- **Stateless vs stateful split** — `NotesScreen` owns the `NotesViewModel`;
  `NotesContent` and everything below it take state as parameters. That split is
  why the `@Preview` functions can render the UI with hand-supplied data and
  never construct a ViewModel.
- **The `@Preview` functions** (`NotesColumnPreview`, `NotesGridPreview`,
  `EmptyStatePreview`) — wrapped in `ComposeListsTheme`, sized 320x480, each
  driving a different state of the *stateless* content.

## Run it

From the project root:

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a running emulator/device
```

Or open the project in Android Studio and press Run. In the app: type in the
search box to filter, tap the toolbar icon to switch between list and grid, and
tap notes to multi-select.

## RecyclerView (historical note)

Before Compose, scrolling lists on Android were built with a **`RecyclerView`**
plus a custom **`Adapter`**, a **`ViewHolder`** for each row, a
**`LayoutManager`** (`LinearLayoutManager` for a list, `GridLayoutManager` for a
grid), and often a `DiffUtil` to compute item changes. That was a lot of
boilerplate just to show a list. **`LazyColumn`** and **`LazyVerticalGrid`**
replace all of it: you write the item composable inline, hand `items(...)` a
stable `key`, and Compose handles recycling, diffing, and layout for you — far
less code for the same (and more capable) result.
