// =============================================================================
// MainActivity.kt
//
// CONCEPT: RELEASE ENGINEERING BASICS — RUNTIME PERMISSIONS
//
// This teaching project is NOT about navigation. It demonstrates the pieces a
// student needs to understand to take a simple notes app from "runs on my
// emulator" toward "shippable to users":
//
//   • A RUNTIME PERMISSION request, using the built-in Jetpack ActivityResult
//     API (no third-party permission library). We request
//     android.permission.POST_NOTIFICATIONS, the permission a notes app would
//     need to post a reminder notification. On Android 13 (API 33) and above
//     this is a *dangerous* permission and must be granted by the user at run
//     time, not just declared in the manifest.
//
//   • The two-step permission model on modern Android:
//        1. DECLARE the permission statically in AndroidManifest.xml.
//        2. REQUEST it at run time (Android 6 / API 23+ for dangerous perms)
//           and react to granted/denied — never assume you have it.
//
// WHAT THE STUDENT SHOULD INSPECT (see README.md for the full tour):
//   • This file: how rememberLauncherForActivityResult + RequestPermission()
//     and ContextCompat.checkSelfPermission work together.
//   • AndroidManifest.xml: the matching <uses-permission> line.
//   • build.gradle.kts: versionCode / versionName, the debug & release build
//     types, and the COMMENTED signingConfigs block (release signing concepts).
// =============================================================================
package com.example.appreleasebasics

import android.Manifest                                          // gives us the Manifest.permission.* string constants
import android.content.pm.PackageManager                         // PERMISSION_GRANTED / PERMISSION_DENIED constants
import android.os.Build                                          // to branch on the device's Android version (SDK_INT)
import android.os.Bundle                                         // the savedInstanceState passed to onCreate
import androidx.activity.ComponentActivity                       // base Activity class for a Compose-only screen
import androidx.activity.compose.rememberLauncherForActivityResult // creates a launcher tied to the composition lifecycle
import androidx.activity.compose.setContent                      // bridges the Activity to a Compose UI tree
import androidx.activity.result.contract.ActivityResultContracts // the catalog of built-in result contracts (RequestPermission, etc.)
import androidx.compose.foundation.layout.Arrangement            // controls spacing between children in a Column/Row
import androidx.compose.foundation.layout.Column                 // vertical layout container
import androidx.compose.foundation.layout.fillMaxSize            // modifier: take all available space
import androidx.compose.foundation.layout.padding                // modifier: add padding around content
import androidx.compose.material3.Button                         // Material 3 filled button
import androidx.compose.material3.MaterialTheme                  // exposes colors + typography from the theme
import androidx.compose.material3.Surface                        // themed background container
import androidx.compose.material3.Text                           // Material 3 text element
import androidx.compose.runtime.Composable                       // marks a function as emitting UI
import androidx.compose.runtime.getValue                         // property-delegate support for State (the `by` keyword)
import androidx.compose.runtime.mutableStateOf                   // creates observable UI state
import androidx.compose.runtime.remember                         // keeps state alive across recompositions
import androidx.compose.runtime.setValue                         // property-delegate support so we can reassign state with `=`
import androidx.compose.ui.Alignment                             // alignment options for layout children
import androidx.compose.ui.Modifier                              // the modifier chain used to decorate composables
import androidx.compose.ui.platform.LocalContext                 // grabs the current Android Context inside a Composable
import androidx.compose.ui.tooling.preview.Preview               // marks a function as an Android Studio design-time preview
import androidx.compose.ui.unit.dp                               // density-independent pixel unit for sizes/spacing
import androidx.core.content.ContextCompat                       // back-compatible checkSelfPermission helper
import com.example.appreleasebasics.ui.theme.AppReleaseBasicsTheme // our Material 3 theme wrapper (see ui/theme/Theme.kt)

// =============================================================================
// ACTIVITY  —  the app's single entry point
// =============================================================================

/**
 * MainActivity hosts the entire Compose UI for the App Release Basics sample.
 *
 * It is declared in AndroidManifest.xml with the MAIN / LAUNCHER intent-filter,
 * which is what makes its icon appear in the launcher and lets Android start it
 * when the user taps that icon.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent replaces the classic setContentView(R.layout.xxx): everything
        // the user sees is described declaratively in Compose below.
        setContent {
            // Wrap the whole app in our theme so MaterialTheme.colorScheme /
            // .typography are available to every child composable.
            AppReleaseBasicsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // The stateful screen owns the permission state and wires up
                    // the real Android permission launcher.
                    NotificationPermissionScreen()
                }
            }
        }
    }
}

// =============================================================================
// STATE + LOGIC  —  the runtime-permission flow (THE concept of this project)
// =============================================================================

/**
 * NotificationPermissionScreen — the STATEFUL screen.
 *
 * It demonstrates the canonical modern runtime-permission flow:
 *
 *   1. Read the CURRENT status with ContextCompat.checkSelfPermission so the UI
 *      can reflect reality on first launch and after returning from settings.
 *   2. Create a permission LAUNCHER with rememberLauncherForActivityResult and
 *      the RequestPermission() contract; its callback receives a Boolean
 *      `granted` from the system permission dialog.
 *   3. On button tap, call launcher.launch(permission) to show the dialog.
 *
 * It then delegates the actual drawing to a STATELESS overload
 * (NotificationPermissionContent) so the visuals can be previewed without
 * touching any real Android Context. See the @Preview functions at the bottom.
 */
@Composable
fun NotificationPermissionScreen() {
    // LocalContext gives us the Activity/Context needed to query permission state.
    val context = LocalContext.current

    // The exact permission string we are demonstrating. POST_NOTIFICATIONS is a
    // dangerous, runtime-granted permission introduced in Android 13 (API 33).
    val permission = Manifest.permission.POST_NOTIFICATIONS

    // ---------------------------------------------------------------------
    // STEP 1: Determine the CURRENT grant status.
    //
    // checkSelfPermission returns PERMISSION_GRANTED or PERMISSION_DENIED.
    // IMPORTANT BACKWARD-COMPAT NOTE: POST_NOTIFICATIONS only EXISTS on API 33+.
    // On API 32 and below there is no such runtime permission — notifications
    // are allowed by default — so we treat older devices as "already granted"
    // to avoid requesting a permission the platform does not recognize.
    // ---------------------------------------------------------------------
    var isGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+: actually ask the system whether we hold the permission.
                ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                // Pre-API-33: the runtime permission does not apply → effectively granted.
                true
            }
        )
    }

    // ---------------------------------------------------------------------
    // STEP 2: Build the permission LAUNCHER.
    //
    // rememberLauncherForActivityResult registers a contract with the Activity
    // Result framework and returns a launcher we can fire later. The
    // RequestPermission() contract shows the system's permission dialog and
    // hands our lambda a Boolean: true if the user granted, false if denied.
    // We update our UI state from that result.
    // ---------------------------------------------------------------------
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // This callback runs on the main thread when the dialog is dismissed.
        isGranted = granted
    }

    // Hand the state + actions to the stateless UI. The "request" action only
    // launches the dialog on API 33+, where the permission is meaningful.
    NotificationPermissionContent(
        isGranted = isGranted,
        onRequestPermission = {
            // STEP 3: actually show the system permission dialog.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(permission)   // <-- the line that triggers the OS dialog
            }
        }
    )
}

// =============================================================================
// UI  —  stateless, preview-friendly rendering
// =============================================================================

/**
 * NotificationPermissionContent — the STATELESS UI.
 *
 * It receives plain data (`isGranted`) and a callback (`onRequestPermission`)
 * and draws the screen. Keeping it free of ViewModels/Context means we can
 * render it in an Android Studio @Preview with hand-supplied values (see below)
 * — you must NEVER build a real launcher or Context inside a @Preview.
 *
 * @param isGranted            whether POST_NOTIFICATIONS is currently held.
 * @param onRequestPermission  invoked when the user taps the request button.
 */
@Composable
fun NotificationPermissionContent(
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title ties the screen back to the notes domain.
        Text(
            text = "My Notes — Reminders",
            style = MaterialTheme.typography.headlineSmall
        )

        // Short explanation of why a notes app wants this permission.
        Text(
            text = "To remind you about a note, this app needs permission to " +
                "post notifications.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Reflect the live permission status to the user. This text recomposes
        // automatically whenever `isGranted` changes (granted/denied).
        Text(
            text = if (isGranted) {
                "Notifications: GRANTED"
            } else {
                "Notifications: DENIED"
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (isGranted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        // The button that kicks off the runtime request. We disable it once the
        // permission is already granted so the user is not prompted needlessly.
        Button(
            onClick = onRequestPermission,
            enabled = !isGranted
        ) {
            Text(if (isGranted) "Permission already granted" else "Request notification permission")
        }
    }
}

// =============================================================================
// PREVIEWS  —  design-time only, no real Context/ViewModel constructed
// =============================================================================

/**
 * Preview of the DENIED state. We feed the STATELESS overload hand-built data
 * (isGranted = false) and a no-op callback, so the preview never touches a real
 * permission launcher or Android Context — both of which are unavailable at
 * design time.
 */
@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotificationPermissionDeniedPreview() {
    AppReleaseBasicsTheme {
        NotificationPermissionContent(
            isGranted = false,
            onRequestPermission = {}   // no-op: previews must not run real side effects
        )
    }
}

/**
 * Preview of the GRANTED state, again using the stateless overload with
 * hand-supplied data so it renders deterministically in Android Studio.
 */
@Preview(showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun NotificationPermissionGrantedPreview() {
    AppReleaseBasicsTheme {
        NotificationPermissionContent(
            isGranted = true,
            onRequestPermission = {}   // no-op: previews must not run real side effects
        )
    }
}
