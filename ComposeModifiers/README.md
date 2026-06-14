# Compose Modifiers

A single-screen Android app that teaches the two tools you use on **every**
composable: the **`Modifier` chain** and **layout** (`Column` / `Row` / `Box` +
`Arrangement`, `Alignment`, and `weight`). Where [`ComposeCatalog`](../ComposeCatalog)
shows you *which* component to reach for, this project shows you how to **size,
space, decorate, and arrange** them.

There is intentionally **no navigation, no networking, and no persistence** — just
one scrolling screen of labelled lessons.

## The one big idea

`Modifier` is an **ordered, immutable chain**. Every call — `.padding(...)`,
`.background(...)`, `.size(...)` — returns a **new** `Modifier` that **wraps** the
previous one, and Compose applies them **outside-in** (first in the chain =
outermost layer). Because each modifier wraps the next, **order changes the
result**:

```kotlin
Modifier.background(Blue).padding(16.dp)   // blue fills everything, text inset inside the blue
Modifier.padding(16.dp).background(Blue)   // 16dp transparent margin, THEN blue hugs the text
```

The headline `@Preview` ("Order: background vs padding") shows both side by side.

## What you see

A scrolling `Scaffold` of lessons:

1. **The modifier chain** — `size → clip → background → padding` read outside-in.
2. **Order matters: background vs padding** — the headline comparison.
3. **Each modifier wraps the next** — `border → padding → border` draws two
   concentric outlines with a visible gap.
4. **Sizing** — `size`, `fillMaxWidth()`, `fillMaxWidth(0.5f)`, `wrapContentWidth`.
5. **Padding variants** — all sides, per-axis, per-side.
6. **Clip, border & shadow** — `clip(CircleShape)`, `border(...)`, `shadow(...)`.
7. **Arrangement (main axis)** — `spacedBy`, `SpaceBetween`, `SpaceAround`,
   `SpaceEvenly`, `Center`, `End`, each on its own row.
8. **Alignment (cross axis)** — `verticalAlignment` Top / Center / Bottom.
9. **weight** — `weight(1f)` vs `weight(2f)` vs a fixed-width child.
10. **Box & `Modifier.align`** — children positioned in corners / center.
11. **Transforms** — `offset`, `alpha`, `rotate`.
12. **aspectRatio** — lock a 16:9 ratio.
13. **clickable** — a modifier that adds *behavior*, not just looks (tap to count).

## Key files

- `src/main/java/com/example/composemodifiers/MainActivity.kt`
  — every lesson, plus the reusable `Lesson` frame and the `Tile` helper (which
  takes a `modifier` parameter — the #1 convention for reusable composables).
- `src/main/java/com/example/composemodifiers/ui/theme/*`
  — `ComposeModifiersTheme`, the Material 3 wrapper.

## What to inspect

- **`Modifier.weight(1f)` and `Modifier.align(...)` are SCOPED** — `weight` only
  exists inside a `RowScope`/`ColumnScope`, and `align` inside a `BoxScope`. They
  resolve because the calls sit lexically inside those layout lambdas.
- **The "order matters" lessons** are best understood by editing the order in the
  source and re-rendering the preview — see the **Try it** note below.
- **`Tile(label, color, modifier)`** forwards its `modifier` to the box it draws,
  so each demo passes in the very modifier it is teaching.

## Run it

From the project root:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # build + install on a device/emulator
./gradlew testDebugUnitTest      # run the example unit test
```

Or open the project in Android Studio and view the two `@Preview`s in the design
pane — the "Order: background vs padding" preview makes the lesson obvious at a
glance, no emulator needed.

> 🧪 **Try it in the browser:** the repo's interactive
> [`Compose Playground`](../homework/playground.html) lets you *edit* a modifier
> chain (swap `.padding()` and `.background()`, change `Arrangement`, add
> `weight`) and watch a live preview update — the fastest way to build intuition.
