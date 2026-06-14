# Compose Catalog

A single-screen Android app that is a guided **tour of the Jetpack Compose +
Material 3 component library**. Where [`ComposeModernUI`](../ComposeModernUI)
teaches *how* Compose works (state hoisting, recomposition, previews), this
project answers the everyday question: **"which widget do I use, and what are its
key parameters?"**

There is intentionally **no navigation, no networking, and no persistence** — just
one scrolling screen that shows every common component as a labelled exhibit.

## What you see

One vertically-scrolling `Scaffold` (with a `TopAppBar`, a `FloatingActionButton`,
and a `Snackbar`) whose body walks through, in order:

1. **Text** — typography roles, color, weight, italic, alignment, `maxLines` +
   ellipsis, underline.
2. **Buttons** — `Button`, `FilledTonalButton`, `ElevatedButton`, `OutlinedButton`,
   `TextButton`, `IconButton`, plus a disabled button and one with a leading icon.
3. **Floating action buttons** — `FloatingActionButton`, `SmallFloatingActionButton`,
   `ExtendedFloatingActionButton`.
4. **Selection controls** — `Checkbox`, a `RadioButton` group, `Switch`.
5. **Sliders** — continuous `Slider`, stepped `Slider`, two-thumb `RangeSlider`.
6. **Text fields** — `TextField` and `OutlinedTextField` (label, placeholder,
   leading icon, single line, password masking, keyboard type).
7. **Containers** — `Surface`, `Card`, `ElevatedCard`, `OutlinedCard`.
8. **Layout primitives** — `Column`, `Row`, `Box`, `Spacer`, `HorizontalDivider`.
9. **Icon & Image** — a tinted vector `Icon` vs. a drawable `Image`.
10. **Progress indicators** — `CircularProgressIndicator` and
    `LinearProgressIndicator`, each indeterminate and determinate.
11. **Chips & badges** — `AssistChip`, `FilterChip`, `SuggestionChip`,
    `BadgedBox` + `Badge`.
12. **Dialog** — an `AlertDialog` opened from a button.
13. **Horizontal list** — a `LazyRow` of cards.

## Key files

- `src/main/java/com/example/composecatalog/MainActivity.kt`
  — the whole catalog: the reusable `ComponentSection` frame, the `CatalogScreen`
  (which owns the demo state), `MainActivity`, and the previews.
- `src/main/java/com/example/composecatalog/ui/theme/*`
  — `ComposeCatalogTheme`, the Material 3 wrapper supplying colors + typography.
- `build.gradle.kts` / `gradle/libs.versions.toml`
  — Compose BOM + Material 3, **plus `material-icons-core`** for the `Icon` demos.

## What to inspect

- **The slot pattern:** `ComponentSection(title, caption) { …demo… }` takes the
  demo UI as a trailing `@Composable` lambda — the same "content slot" idea that
  `Button { }`, `Card { }`, and `Scaffold` itself use.
- **State for the interactive controls** lives in `CatalogScreen` via
  `remember { mutableStateOf(...) }`. (Where state *should* live — hoisted into a
  stateless child or a ViewModel — is the lesson of `ComposeModernUI` /
  `NavViewModelState`; here we focus on the widgets.)
- **The determinate progress lambda:** `CircularProgressIndicator(progress = { 0.66f })`
  — modern Material 3 takes `progress` as a lambda, not a bare `Float`.
- **`leadingIcon = if (selected) { { Icon(...) } } else null`** on the `FilterChip`
  — a nullable composable-lambda slot.

## Run it

From the project root:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # build + install on a device/emulator
./gradlew testDebugUnitTest      # run the example unit test
```

Or open the project in Android Studio and view the `Catalog` `@Preview` in the
design pane — the whole screen renders without an emulator.

> 🧪 **Try it in the browser too:** the repo ships an interactive
> [`Compose Playground`](../homework/playground.html) where you can *edit* Compose
> code for these very components and watch a live preview update as you type.
