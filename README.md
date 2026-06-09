# Android Studio Projects — Compose Navigation by example

A collection of small, self-contained **Jetpack Compose** apps built as a teaching
progression for Android navigation. They start from a single static screen and
build up to a full set of **Navigation 3** patterns — one concept per project, so
each app stays small enough to read top-to-bottom.

Most projects share the same **"planets" domain** (`Category` → `Item`) and the
same heavy, line-by-line teaching comments, so you can diff any two projects to
see exactly what a feature adds.

> 📚 For the navigation curriculum in depth — suggested teaching order, the exact
> `file:line` where each concept lives, and run/test notes — see
> **[`NAV_DEMOS.md`](./NAV_DEMOS.md)**.

---

## The learning path

### 1 · Compose foundation
| Project | What it shows |
|---|---|
| [`SingleActivity`](./SingleActivity) | The smallest possible Compose app: one `Activity`, one `setContent`, a single `Greeting` — no navigation at all. The starting point. |

### 2 · First steps with Navigation 3 (two screens)
| Project | What it shows |
|---|---|
| [`Intent`](./Intent) | Home → Detail, where a Nav3 key **argument** carries data — the modern replacement for the classic `Intent` extra (`putExtra` / `getStringExtra`). |
| [`NavDetailList`](./NavDetailList) | A list → detail variant. |
| [`NavListDetail`](./NavListDetail) | The canonical list → detail screen, extensively commented — the reference for the pattern. |

### 3 · Growing the back stack
| Project | What it shows |
|---|---|
| [`NavThreeScreen`](./NavThreeScreen) | A 3-deep drill-down: Categories → Items → Detail, with exhaustive `@PreviewParameter` previews. |
| [`NavFourScreen`](./NavFourScreen) | Adds a 4th screen reached by a **Button**, plus a multi-level "Start over" pop. The base the concept demos below are cloned from. |

### 4 · One navigation concept per project
| Project | Concept |
|---|---|
| [`NavDeepLinks`](./NavDeepLinks) | Open a screen directly from a URI; hand-seed a back stack so Back still works. |
| [`NavBottomTabs`](./NavBottomTabs) | Bottom tabs where **each tab owns its own back stack** (multiple back stacks). |
| [`NavNestedGraphs`](./NavNestedGraphs) | A self-contained sub-flow (onboarding) that pops as a unit, then hands off to the main flow. |
| [`NavViewModelState`](./NavViewModelState) | State/logic in a `ViewModel` (`StateFlow`); survives rotation & navigation, unlike `remember`. |
| [`NavTransitions`](./NavTransitions) | Custom animated screen transitions (forward / pop / predictive-back). |
| [`NavDataLayer`](./NavDataLayer) | Screens observe a **repository through a ViewModel** with loading / empty / error / success states. |

---

## Running a project

Each folder is an independent Android project.

- **Android Studio:** `File ▸ Open…` and select the project folder (e.g. `NavFourScreen`).
- **Command line** (from inside a project folder):
  ```bash
  ./gradlew installDebug      # build & install on a running device/emulator
  ./gradlew :compileDebugKotlin   # just type-check / compile
  ```

**Deep link demo** (`NavDeepLinks`, with the app installed):
```bash
adb shell am start -W -a android.intent.action.VIEW \
  -d "navdemo://planet/4" com.example.navdeeplinks
```
Opens straight to the **Mars** detail screen, with Back walking up to Items → Categories.

---

## Tech stack

Jetpack Compose · **Navigation 3** (`androidx.navigation3`) · Material 3 ·
Kotlin `2.2.10` · AGP `9.2.1` · `compileSdk 37` / `minSdk 24` · Compose BOM `2026.02.01`.
`NavViewModelState` and `NavDataLayer` additionally use
`lifecycle-viewmodel-compose` + `lifecycle-runtime-compose`.

## Cloning notes

`local.properties` is intentionally **not** committed (it holds a machine-specific
SDK path). After cloning, open a project in Android Studio — it generates the file
automatically — or set `sdk.dir` / the `ANDROID_HOME` environment variable yourself.
