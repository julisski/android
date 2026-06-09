# Compose Modern UI

A single-screen Android app (the **notes** domain) that teaches the **fundamentals
of Jetpack Compose + Material 3**. There is intentionally **no navigation, no
networking, and no persistence** — the whole point is to focus on building UI.

## Learning goal

Learn how modern Android UI is built declaratively with Jetpack Compose:

- **Composables** — UI is described by `@Composable` functions; you don't mutate
  views, you re-run functions and Compose "recomposes" what changed.
- **The Material 3 theme** — colors come from `MaterialTheme.colorScheme` and
  text styles from `MaterialTheme.typography`, never hardcoded values.
- **Layout** — `Column` (vertical), `Row` (horizontal), `Box` (overlap), plus
  spacing (`Spacer`, `padding`, `Arrangement.spacedBy`) and `Alignment`.
- **State hoisting** — the headline lesson: a **stateless** child takes its value
  as a parameter and reports changes via a callback, while the **parent owns the
  state**. State flows **down**, events flow **up**.
- **Reusable components** — small, single-purpose composables you compose together
  and preview in isolation.
- **`@Preview`** — render UI in Android Studio's design pane without an emulator.

## Key files

- `src/main/java/com/example/composemodernui/MainActivity.kt`
  — the entire lesson: data model, reusable components, the state-hoisting demo,
  the two `NotesScreen` overloads (stateless + stateful), and the previews.
- `src/main/java/com/example/composemodernui/ui/theme/Theme.kt`
  — `ComposeModernUITheme`, the Material 3 wrapper supplying colors + typography.
- `src/main/java/com/example/composemodernui/ui/theme/Color.kt` / `Type.kt`
  — the palette and the type scale the theme draws from.
- `build.gradle.kts` / `gradle/libs.versions.toml`
  — dependencies: Compose BOM + Material 3 only (Navigation 3 and serialization
  from the base sample were removed).

## What to inspect

- **State hoisting (the main event):** open `FavoriteToggle`. It takes
  `isFavorite: Boolean` (state **down**) and `onToggle: (Boolean) -> Unit`
  (event **up**) and stores **nothing**. The value lives in the parent. Read the
  big `STATEFUL vs STATELESS` comment block right above it.
- **Two `NotesScreen` overloads:** the **stateless** one is pure parameters +
  callbacks; the **stateful** one is the only place that calls
  `rememberSaveable` and owns `notes`, `newTitle`, and `isFavorite`. This is the
  canonical "state holder vs. stateless content" split.
- **A hoisted text input:** the `OutlinedTextField` has no internal text — its
  `value` is the hoisted `newTitle` and `onValueChange` sends each keystroke up.
  This is "event down / state up" on an input.
- **Reusable components:** `NoteCard`, `SectionHeader`, and `PrimaryButton` each
  do one thing and pull their look from the theme.
- **Previews:** all three `@Preview`s call the **stateless** screen with
  hand-supplied data (and one previews a single `NoteCard`). A preview **never**
  constructs the stateful screen or a ViewModel — that's the payoff of hoisting,
  and it lets us render both the "favorited" and "not favorite" states by just
  passing a literal.
- **Theme-driven styling:** notice there are no hardcoded colors or font sizes in
  the components — everything reads from `MaterialTheme.colorScheme` /
  `MaterialTheme.typography`, so light/dark and dynamic color all work for free.

## Run it

From the project root:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew testDebugUnitTest      # run the example unit test
```

Or open the project in Android Studio, then use the design pane to view the
`@Preview` functions in `MainActivity.kt` without launching an emulator.
