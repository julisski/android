# Location Services

A single-screen Jetpack Compose app that asks for the **runtime location permission**,
then reads the device's position from Google Play services' **fused location provider**
and renders explicit **Idle / Loading / Success / Error** states. It teaches the
end-to-end "request a dangerous permission, integrate a device API, show it safely"
flow using a **ViewModel** exposing a `StateFlow`.

## Learning goal

Learn how an Android app **integrates a device/location API** and how to surface the
result as clear UI states:

- **Runtime permissions** — `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` are
  *dangerous* permissions: declare them in the manifest **and** request them at run
  time with the Jetpack `ActivityResult` API (`RequestMultiplePermissions`), then react
  to the user's choice. The user may grant only coarse, only fine, or neither.
- **FusedLocationProviderClient** — the modern, battery-efficient device-location API.
  Its callback-based `Task` is adapted into a clean `suspend` function with
  `suspendCancellableCoroutine`, so callers just `currentLocation()`.
- **DTO vs domain model** — keep the framework `android.location.Location` out of the
  app; the repository maps it into a small plain `LocationData` (latitude, longitude).
- **The UiState pattern** — model the screen as a sealed `LocationUiState`
  (`Idle` / `Loading` / `Success(location)` / `Error(message)`) exposed via `StateFlow`.
- **Error handling** — a single `try/catch` in the ViewModel turns any failure (no fix,
  location disabled, permission denied) into an `Error` state instead of a crash.
- **Real vs Fake switch** — a repository interface with a GPS-backed `Real` impl and an
  in-memory `Fake` impl, swapped by one flag so the app and tests run with no GPS.

## Key files

- `src/main/java/com/example/locationservices/LocationData.kt` — the plain domain model.
- `src/main/java/com/example/locationservices/LocationRepository.kt` — the
  `LocationRepository` interface plus `RealLocationRepository`
  (`FusedLocationProviderClient`) and `FakeLocationRepository` (offline), and
  `provideLocationRepository(context, useFake = …)` — **the switch**.
- `src/main/java/com/example/locationservices/LocationViewModel.kt` — `LocationUiState`
  sealed interface and the ViewModel that fetches a fix into a `StateFlow`.
- `src/main/java/com/example/locationservices/MainActivity.kt` — the Compose UI:
  `LocationScreen` (permission launcher + collects the StateFlow) and the stateless
  `LocationContent` that renders each state, plus `@Preview`s.
- `src/test/java/com/example/locationservices/ExampleUnitTest.kt` — unit tests for the
  fake repository and the ViewModel state machine (with a `MainDispatcherRule`).
- `src/main/AndroidManifest.xml` — the two `<uses-permission>` location lines.

## What to inspect

- **`MainActivity.kt`**: the `RequestMultiplePermissions` launcher and how its
  `Map<String, Boolean>` result decides whether to fetch a location or show
  "permission denied"; `checkSelfPermission()` seeds the current grant status on launch.
- **`LocationRepository.kt`**: how `getCurrentLocation(...)`'s success/failure listeners
  are bridged into a `suspend` function, and the `@SuppressLint("MissingPermission")`
  that is safe because the UI checks permission first.
- **`LocationViewModel.kt`**: the private `MutableStateFlow` vs the public read-only
  `StateFlow`, and that we do **not** fetch in `init` — a location read must wait until
  permission is granted, so the screen calls `fetchLocation()` only after that.

## Run it

By default the app runs **offline** in fake mode
(`provideLocationRepository(context, useFake = true)`), so it builds and runs with **no
GPS and no Play services** (e.g. on CI or a bare emulator), and the unit tests need no
hardware. The runtime-permission flow itself is fully real; only the returned coordinate
is faked (the Googleplex, after a short delay so you can see the Loading spinner).

To read the **real device location**:

1. In `LocationRepository.kt`, change the default to `useFake = false`.
2. Run on a device/emulator with location enabled and **grant the permission** when asked.

Build from the project root:

```bash
./gradlew assembleDebug --console=plain --stacktrace
./gradlew testDebugUnitTest
```
