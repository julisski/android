// =============================================================================
// LocationRepository.kt  —  REPOSITORY: one source of truth, REAL vs FAKE
//
// CONCEPT THIS FILE TEACHES: integrating a DEVICE API (the fused location
// provider from Google Play services) behind a small interface, so the rest of
// the app asks "what is my current location?" without knowing about GPS,
// CancellationToken, or callback listeners. Behind the interface sits either:
//   • RealLocationRepository — wraps FusedLocationProviderClient and asks the
//     hardware for a fresh fix (needs a granted location permission + Play
//     services), or
//   • FakeLocationRepository — returns a fixed, hardcoded coordinate after a
//     short delay, so the app and the unit tests run with NO GPS and NO Play
//     services (e.g. on CI or a bare emulator image).
//
// THE SWITCH provideLocationRepository(useFake = ...) chooses between them in
// exactly one place — the same pattern the NetworkParsing demo uses for online
// vs offline. The default is FAKE so the project builds, runs, and tests
// everywhere; flip it to FALSE on a real device to read the actual GPS.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. The LocationRepository interface — the single method currentLocation().
//   2. RealLocationRepository — how a CALLBACK-based Play services Task is turned
//      into a clean `suspend` function with suspendCancellableCoroutine.
//   3. FakeLocationRepository — deterministic, offline, no permission needed.
//   4. provideLocationRepository(context, useFake) — THE SWITCH.
// =============================================================================
package com.example.locationservices

import android.annotation.SuppressLint                       // to silence the permission lint on the real call (we check first)
import android.content.Context                               // needed to obtain the fused location client
import com.google.android.gms.location.FusedLocationProviderClient // the device-location API client
import com.google.android.gms.location.LocationServices      // factory for the fused location client
import com.google.android.gms.location.Priority              // accuracy/power trade-off for a request
import com.google.android.gms.tasks.CancellationTokenSource  // lets us cancel an in-flight location request
import kotlinx.coroutines.delay                              // non-blocking pause used by the fake
import kotlinx.coroutines.suspendCancellableCoroutine        // bridges a callback Task into a suspend function
import kotlin.coroutines.resume                              // resume the coroutine with a value
import kotlin.coroutines.resumeWithException                 // resume the coroutine by throwing

// ===========================================================================
// REPOSITORY CONTRACT  —  what callers (the ViewModel) depend on
// ===========================================================================

/**
 * LocationRepository — the abstraction the ViewModel talks to. It hides whether
 * the coordinate comes from real GPS hardware or from a hardcoded fake.
 */
interface LocationRepository {
    /**
     * Return the device's current [LocationData]. `suspend` because the real
     * implementation waits for the hardware to produce a fix. May THROW (e.g. no
     * fix available, location disabled) — the ViewModel catches that and turns it
     * into an Error UI state.
     */
    suspend fun currentLocation(): LocationData
}

// ===========================================================================
// REAL IMPLEMENTATION  —  FusedLocationProviderClient -> suspend -> domain model
// ===========================================================================

/**
 * RealLocationRepository — asks Google Play services for a single, fresh, high-
 * accuracy fix and maps the framework Location into our [LocationData].
 *
 * Play services exposes this as a callback-based Task; we adapt it to a coroutine
 * with [suspendCancellableCoroutine] so callers just `currentLocation()`.
 *
 * @property client the fused location client (defaulted from a [Context]).
 */
class RealLocationRepository(
    private val client: FusedLocationProviderClient,
) : LocationRepository {

    // Convenience constructor: build the client straight from a Context.
    constructor(context: Context) : this(
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    )

    // The caller (the UI) guarantees a location permission was granted BEFORE this
    // runs, so we suppress the "MissingPermission" lint on the getCurrentLocation call.
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): LocationData =
        suspendCancellableCoroutine { continuation ->
            // A token lets us cancel the request if the coroutine is cancelled.
            val cancellationSource = CancellationTokenSource()

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Map the framework Location -> our clean domain model.
                        continuation.resume(LocationData(location.latitude, location.longitude))
                    } else {
                        // The provider can legitimately return null (no fix yet).
                        continuation.resumeWithException(
                            IllegalStateException("No location fix is available right now.")
                        )
                    }
                }
                .addOnFailureListener { error ->
                    // Surface the failure so the ViewModel can show an Error state.
                    continuation.resumeWithException(error)
                }

            // If the coroutine is cancelled, cancel the underlying Play services request.
            continuation.invokeOnCancellation { cancellationSource.cancel() }
        }
}

// ===========================================================================
// FAKE IMPLEMENTATION  —  offline, deterministic, no permission, no GPS
// ===========================================================================

/**
 * FakeLocationRepository — returns the same fixed coordinate every time, after a
 * short [delay] to imitate the time a real fix takes (so you can SEE the Loading
 * state). Touches no hardware, so it works on CI and in unit tests.
 *
 * The default coordinate is the Googleplex in Mountain View, CA.
 *
 * @property fixed the coordinate to return.
 */
class FakeLocationRepository(
    private val fixed: LocationData = LocationData(latitude = 37.4220, longitude = -122.0841),
) : LocationRepository {

    override suspend fun currentLocation(): LocationData {
        delay(600)            // imitate the latency of acquiring a real fix
        return fixed
    }
}

// ===========================================================================
// THE SWITCH  —  pick REAL (device GPS) or FAKE (offline) in one place
// ===========================================================================

/**
 * Factory that returns the repository the app should use.
 *
 * @param context used to build the real fused-location client when needed.
 * @param useFake when TRUE (the default), returns the offline [FakeLocationRepository]
 *   so the project builds, runs, and tests with no GPS/Play services. Flip it to
 *   FALSE on a real device (with location permission granted) to read actual GPS.
 *
 *   ┌──────────────────────────────────────────────────────────────────────────┐
 *   │  STUDENTS: change this single argument to toggle between fake and device.  │
 *   └──────────────────────────────────────────────────────────────────────────┘
 */
fun provideLocationRepository(context: Context, useFake: Boolean = true): LocationRepository =
    if (useFake) FakeLocationRepository()          // offline default: no GPS required
    else RealLocationRepository(context)           // live: requires permission + Play services
