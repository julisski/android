# App Release Basics

A small, single-screen Jetpack Compose notes app whose only job is to teach the
**release-engineering fundamentals** every Android app needs before it can ship:
runtime permissions, app versioning, build types, release signing, and the
Android App Bundle (AAB). **No Google Play Console account or credentials are
required to learn any of this.**

## Learning goal

Understand the pieces that turn "it runs on my emulator" into "it could be
published":

- **Runtime permissions** — declare a permission in the manifest *and* request
  it at run time (Android 6 / API 23+ for dangerous permissions), then react to
  granted/denied. We use `POST_NOTIFICATIONS` (a dangerous permission on Android
  13 / API 33+) via the built-in Jetpack **ActivityResult** API — no third-party
  permission library.
- **Versioning** — what `versionCode` and `versionName` mean and when to bump.
- **Build types** — how `debug` and `release` differ.
- **Release signing** — conceptually, how a keystore + `signingConfigs` is wired
  in (shown as commented guidance, so the debug build still works with no key).
- **AAB** — how `bundleRelease` produces the artifact Google Play wants, once a
  keystore is configured.

## Key files

- `build.gradle.kts` — the heart of the lesson: `versionCode` / `versionName`,
  the `debug` and `release` build types, and a **commented `signingConfigs`
  example** showing exactly where a release keystore and passwords would go.
- `src/main/AndroidManifest.xml` — the `<uses-permission>` for
  `POST_NOTIFICATIONS`, plus commented notes on the app `label`/`icon`/`theme`
  and the `MAIN`/`LAUNCHER` `<intent-filter>` that makes the app appear in the
  drawer.
- `src/main/java/com/example/appreleasebasics/MainActivity.kt` — the permission
  screen. `NotificationPermissionScreen` owns the state and the launcher;
  `NotificationPermissionContent` is the stateless, preview-friendly UI.
- `proguard-rules.pro` — where R8/ProGuard keep-rules would live for a minified
  release (currently unused because minification is off).

## What to inspect

- In **MainActivity.kt**:
  - `ContextCompat.checkSelfPermission(...)` — reading the *current* grant status
    so the UI reflects reality on launch.
  - `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> ... }`
    — the built-in permission launcher; its lambda receives the Boolean result.
  - `launcher.launch(permission)` — the single line that shows the OS dialog.
  - The `Build.VERSION.SDK_INT >= TIRAMISU` guards — `POST_NOTIFICATIONS` only
    exists on API 33+, so older devices are treated as already granted.
  - The two `@Preview` functions render the **stateless** overload with
    hand-supplied data — never construct a launcher/ViewModel inside a preview.
- In **AndroidManifest.xml**: confirm the permission is declared *and* notice
  that declaration alone does not grant it on API 33+ — the runtime request does.
- In **build.gradle.kts**: read the `versionCode`/`versionName` comments and the
  commented `signingConfigs` + `release` block (and the note about turning on
  `isMinifyEnabled` / `isShrinkResources` for real releases).

## Run it

```bash
./gradlew assembleDebug        # builds a debug APK (auto-signed with the debug key)
./gradlew testDebugUnitTest    # runs the example unit test
```

Install the debug APK on an Android 13+ device/emulator, tap **Request
notification permission**, and watch the status flip between DENIED and GRANTED.

## Publishing concepts (no Play Console needed)

These are concepts to learn — you do **not** need any store account or
credentials to follow along.

- **`versionCode` vs `versionName`**
  - `versionCode` is an **integer** that must strictly increase with every build
    you upload; Google Play uses *only* this to decide which build is newer.
    Users never see it. Bump it (e.g. `1` → `2`) on every published release.
  - `versionName` is a **human-readable string** (e.g. `"1.0"`, `"1.2.3"`) shown
    to users. It is not used for ordering; bump it however your scheme likes
    (semver is common).

- **Debug vs release build types**
  - **debug** is auto-signed with a throwaway debug key, is debuggable, and skips
    code shrinking — fast to build, but **never shippable**.
  - **release** is what you ship: signed with *your* keystore, and typically
    minified/shrunk with R8 (`isMinifyEnabled = true` + `isShrinkResources =
    true`) to reduce size and deter reverse-engineering.

- **Generate a keystore with `keytool` and wire up `signingConfigs`**
  1. Create a keystore (keep it safe and backed up — losing it means you can
     never update the app under the same signature):
     ```bash
     keytool -genkeypair -v -keystore release.jks -alias myalias \
       -keyalg RSA -keysize 2048 -validity 10000
     ```
  2. Store the passwords **outside source control** (e.g. environment variables
     or `~/.gradle/gradle.properties`) — never hardcode them in `build.gradle.kts`.
  3. Uncomment the `signingConfigs { create("release") { ... } }` block in
     `build.gradle.kts` and attach it with
     `signingConfig = signingConfigs.getByName("release")` inside `release {}`.

- **Generate an AAB with `bundleRelease`**
  - Once a keystore is configured, run:
    ```bash
    ./gradlew bundleRelease
    ```
  - This produces an **Android App Bundle** (`.aab`) under
    `build/outputs/bundle/release/`. The AAB is the format Google Play prefers;
    Play generates optimized, per-device APKs from it.
  - This repo does **not** run `bundleRelease` as part of its build because no
    real signing keys are configured — that step is yours to try once you have a
    keystore.
