// =============================================================================
// LocationViewModel.kt  —  UI STATE: Idle / Loading / Success / Error via StateFlow
//
// CONCEPT THIS FILE TEACHES: a location screen is never just "the coordinate". At
// any moment it is in ONE of four states — IDLE (haven't asked yet), LOADING
// (waiting for a fix), SUCCESS(with a LocationData), or ERROR(with a message).
// Model those with a SEALED INTERFACE, expose the current one as a StateFlow, and
// the UI becomes a pure function of that state.
//
// WHAT THE STUDENT SHOULD INSPECT HERE:
//   1. LocationUiState — a sealed interface with exactly four cases.
//   2. _uiState (MutableStateFlow, private) vs uiState (StateFlow, public, read-only).
//   3. fetchLocation() — launches in viewModelScope, sets Loading, then try/catch
//      sets Success or Error. The try/catch is THE error-handling boundary.
// =============================================================================
package com.example.locationservices

import androidx.lifecycle.ViewModel                          // base class that survives configuration changes
import androidx.lifecycle.viewModelScope                     // coroutine scope tied to this ViewModel's lifetime
import kotlinx.coroutines.flow.MutableStateFlow              // read/write reactive state holder
import kotlinx.coroutines.flow.StateFlow                     // read-only view exposed to the UI
import kotlinx.coroutines.flow.asStateFlow                   // exposes the mutable flow as immutable
import kotlinx.coroutines.launch                             // starts the (suspending) location fetch

// ===========================================================================
// STATE  —  the four things the screen can be, made into a closed type
// ===========================================================================

/**
 * LocationUiState — the complete set of states the location screen can be in.
 * Exactly one is active at any time. Sealed, so a `when` over it is exhaustive.
 */
sealed interface LocationUiState {
    /** Nothing requested yet — show the explanatory prompt + the request button. */
    data object Idle : LocationUiState

    /** A fix is being acquired; show a spinner. */
    data object Loading : LocationUiState

    /**
     * A fix was obtained.
     * @property location the coordinate to display.
     */
    data class Success(val location: LocationData) : LocationUiState

    /**
     * Acquiring a fix failed (or permission was denied).
     * @property message a human-readable reason to show alongside a Retry button.
     */
    data class Error(val message: String) : LocationUiState
}

// ===========================================================================
// VIEWMODEL  —  owns the state and the fetch logic
// ===========================================================================

/**
 * LocationViewModel — fetches the current location from a [LocationRepository] and
 * publishes the current [LocationUiState] as a [StateFlow] the screen observes.
 *
 * The repository is injected (defaulting to the offline fake) so the ViewModel
 * never hard-codes WHERE the location comes from — and so tests can pass a
 * controlled fake. Unlike the networking demo, we do NOT fetch in `init`: a
 * location read must wait until the user has granted permission, so the screen
 * calls [fetchLocation] only after permission is in hand.
 *
 * @param repository the data source; defaults to the offline [FakeLocationRepository].
 */
class LocationViewModel(
    private val repository: LocationRepository = FakeLocationRepository(),
) : ViewModel() {

    // PRIVATE mutable state: only the ViewModel may change it. Starts Idle because
    // nothing is requested until the user taps the button (after granting permission).
    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)

    // PUBLIC read-only state: the screen collects this but cannot mutate it.
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    /**
     * Acquire the current location. Safe to call again — e.g. from a Retry button.
     *
     * Flow: set Loading -> try the (suspending) repository call -> on success
     * publish Success(location); on ANY exception publish Error(message). This
     * try/catch is the single place a failed fix becomes a visible UI state.
     */
    fun fetchLocation() {
        viewModelScope.launch {
            _uiState.value = LocationUiState.Loading             // 1) show the spinner
            try {
                val location = repository.currentLocation()      // 2) suspend: read the fix
                _uiState.value = LocationUiState.Success(location) // 3a) publish the coordinate
            } catch (e: Exception) {
                // 3b) ERROR HANDLING: any failure (no fix, location off, ...) becomes
                // an Error state with a readable message instead of a crash.
                _uiState.value = LocationUiState.Error(
                    e.message ?: "Something went wrong while getting your location."
                )
            }
        }
    }

    /**
     * Record that the user DENIED the location permission, so the screen can show
     * why it cannot read a location. Called from the permission-result callback.
     */
    fun onPermissionDenied() {
        _uiState.value = LocationUiState.Error(
            "Location permission is required to show your position."
        )
    }
}
