// =============================================================================
// MainActivity.kt  —  requesting the LOCATION permission, then showing a fix
//
// CONCEPT THIS PROJECT TEACHES: INTEGRATING A DEVICE API (location services). Two
// things have to happen, in order:
//   1. RUNTIME PERMISSION — ACCESS_FINE/COARSE_LOCATION are *dangerous* permissions,
//      so we must request them at run time with the Jetpack ActivityResult API and
//      react to the user's choice (this file).
//   2. READ THE LOCATION — once permission is in hand, ask the fused location
//      provider for a fix and surface LOADING / SUCCESS / ERROR (LocationViewModel
//      + LocationRepository).
//
// This file is the UI layer; the data layer is split across:
//   • LocationData.kt       — the plain domain model (latitude, longitude).
//   • LocationRepository.kt  — REAL (FusedLocationProviderClient) vs FAKE + the switch.
//   • LocationViewModel.kt   — LocationUiState sealed interface + StateFlow + fetch logic.
//
// WHAT THE STUDENT SHOULD INSPECT IN THIS FILE:
//   1. The RequestMultiplePermissions launcher and how its Map<String,Boolean>
//      result drives whether we fetch a location or show "permission denied".
//   2. checkSelfPermission() used to reflect the CURRENT grant status on launch.
//   3. LocationContent() — a STATELESS overload that just renders a LocationUiState,
//      so the @Previews can drive each state with no real Context/permission.
// =============================================================================
package com.example.locationservices

import android.Manifest                                       // gives us the Manifest.permission.* string constants
import android.content.pm.PackageManager                      // PERMISSION_GRANTED / PERMISSION_DENIED constants
import android.os.Bundle                                      // savedInstanceState type passed to onCreate
import androidx.activity.ComponentActivity                    // base Activity class with Compose support
import androidx.activity.compose.rememberLauncherForActivityResult // creates a launcher tied to the composition
import androidx.activity.compose.setContent                   // bridges an Activity to a Compose UI tree
import androidx.activity.enableEdgeToEdge                     // lets the app draw behind the system bars
import androidx.activity.result.contract.ActivityResultContracts // catalog of built-in result contracts
import androidx.compose.foundation.layout.Arrangement         // spacing between children in a row/column
import androidx.compose.foundation.layout.Column              // stacks children vertically
import androidx.compose.foundation.layout.Spacer              // empty box used to add fixed gaps
import androidx.compose.foundation.layout.fillMaxSize         // modifier: take all available space
import androidx.compose.foundation.layout.height              // modifier: force a specific height
import androidx.compose.foundation.layout.padding             // modifier: add space around content
import androidx.compose.material3.Button                      // filled, tappable button
import androidx.compose.material3.CircularProgressIndicator   // the spinner shown during Loading
import androidx.compose.material3.MaterialTheme               // access to the current theme's colors/typography
import androidx.compose.material3.Scaffold                    // standard screen frame (handles insets)
import androidx.compose.material3.Text                        // draws text
import androidx.compose.runtime.Composable                    // marks a function as emitting UI
import androidx.compose.runtime.getValue                      // enables `val x by …` delegation for State
import androidx.compose.runtime.mutableStateOf                // creates observable UI state
import androidx.compose.runtime.remember                      // keeps state alive across recompositions
import androidx.compose.runtime.setValue                      // enables reassigning `by` state with `=`
import androidx.compose.ui.Alignment                          // align children (e.g. center the spinner)
import androidx.compose.ui.Modifier                           // the "how to lay out / decorate" object
import androidx.compose.ui.platform.LocalContext              // grabs the current Android Context inside a Composable
import androidx.compose.ui.tooling.preview.Preview            // enables @Preview rendering in Android Studio
import androidx.compose.ui.unit.dp                            // density-independent pixel unit (e.g. 16.dp)
import androidx.core.content.ContextCompat                    // back-compatible checkSelfPermission helper
import androidx.lifecycle.compose.collectAsStateWithLifecycle // observe a StateFlow safely w.r.t. lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel         // obtain a ViewModel from inside a @Composable
import com.example.locationservices.ui.theme.LocationServicesTheme // our app's Material theme wrapper

/**
 * MainActivity — the app's single Activity and the entry point Android launches.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationServicesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LocationScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// The two location permissions we request together. Asking for both lets the user
// grant precise (fine) OR approximate (coarse) — either is enough for this demo.
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

// ===========================================================================
// UI  —  STATEFUL screen: owns permission state, drives the ViewModel
// ===========================================================================

/**
 * LocationScreen — the STATEFUL entry composable. It:
 *   1. obtains a [LocationViewModel] and observes its StateFlow,
 *   2. tracks whether a location permission is currently held,
 *   3. builds a permission launcher whose result either fetches a location or
 *      records a denial, and
 *   4. delegates ALL drawing to the stateless [LocationContent].
 */
@Composable
fun LocationScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(),               // survives recomposition/rotation
) {
    val context = LocalContext.current

    // Observe the ViewModel's state lifecycle-awarely (pauses while not visible).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Whether we currently hold EITHER location permission. Seeded from the real
    // grant status so the UI is correct on first launch and after returning from
    // Settings; updated by the launcher callback below.
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // Build the permission LAUNCHER. RequestMultiplePermissions shows the system
    // dialog for several permissions at once and hands us a Map<permission, granted>.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // The user may grant only coarse, only fine, or neither. ANY granted permission
        // is enough for us to read an (approximate or precise) location.
        val granted = results.values.any { it }
        hasPermission = granted
        if (granted) {
            viewModel.fetchLocation()        // permission in hand -> read the location
        } else {
            viewModel.onPermissionDenied()   // explain why we cannot proceed
        }
    }

    LocationContent(
        uiState = uiState,
        hasPermission = hasPermission,
        onGetLocation = {
            // If we already hold a permission, go straight to reading the location;
            // otherwise launch the system permission dialog first.
            if (hasPermission) {
                viewModel.fetchLocation()
            } else {
                launcher.launch(LOCATION_PERMISSIONS)
            }
        },
        modifier = modifier,
    )
}

// ===========================================================================
// UI  —  STATELESS renderer: a pure function of (state, permission)
// ===========================================================================

/**
 * LocationContent — the STATELESS UI. Given a [LocationUiState] and whether a
 * permission is held, it draws the matching screen and calls [onGetLocation] when
 * the button is tapped. It holds NO state and builds NO launcher/Context, which is
 * exactly why the @Previews can drive it directly.
 */
@Composable
fun LocationContent(
    uiState: LocationUiState,
    hasPermission: Boolean,
    onGetLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Where am I?", style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "This app reads your device location through Google Play services' " +
                "fused location provider — after you grant the location permission.",
            style = MaterialTheme.typography.bodyMedium,
        )

        // Reflect the live permission status to the user.
        Text(
            text = if (hasPermission) "Location permission: GRANTED" else "Location permission: NOT GRANTED",
            style = MaterialTheme.typography.titleMedium,
            color = if (hasPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // The body changes with the state: an exhaustive `when` over the sealed type.
        when (uiState) {
            is LocationUiState.Idle ->
                Text(
                    text = "Tap the button to find your location.",
                    style = MaterialTheme.typography.bodyMedium,
                )

            is LocationUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Getting your location…", style = MaterialTheme.typography.bodyMedium)
            }

            is LocationUiState.Success ->
                Text(
                    // %.5f ≈ ~1 metre of precision — plenty for showing a coordinate.
                    text = "Latitude: %.5f\nLongitude: %.5f".format(
                        uiState.location.latitude,
                        uiState.location.longitude,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )

            is LocationUiState.Error ->
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The button label reflects whether we still need permission first.
        Button(onClick = onGetLocation, enabled = uiState !is LocationUiState.Loading) {
            Text(
                when {
                    !hasPermission -> "Grant permission & get location"
                    uiState is LocationUiState.Success || uiState is LocationUiState.Error -> "Refresh location"
                    else -> "Get my location"
                }
            )
        }
    }
}

// ===========================================================================
// PREVIEWS  —  render each state with hand-supplied data (no real Context)
// ===========================================================================

@Preview(name = "Idle (no permission)", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LocationIdlePreview() {
    LocationServicesTheme {
        LocationContent(uiState = LocationUiState.Idle, hasPermission = false, onGetLocation = {})
    }
}

@Preview(name = "Loading", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LocationLoadingPreview() {
    LocationServicesTheme {
        LocationContent(uiState = LocationUiState.Loading, hasPermission = true, onGetLocation = {})
    }
}

@Preview(name = "Success", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LocationSuccessPreview() {
    LocationServicesTheme {
        LocationContent(
            uiState = LocationUiState.Success(LocationData(37.42200, -122.08410)),
            hasPermission = true,
            onGetLocation = {},
        )
    }
}

@Preview(name = "Error", showBackground = true, widthDp = 320, heightDp = 480)
@Composable
fun LocationErrorPreview() {
    LocationServicesTheme {
        LocationContent(
            uiState = LocationUiState.Error("Location permission is required to show your position."),
            hasPermission = false,
            onGetLocation = {},
        )
    }
}
