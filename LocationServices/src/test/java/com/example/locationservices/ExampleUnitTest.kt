// =============================================================================
// ExampleUnitTest.kt  —  local JVM unit tests for the offline location data layer
//
// These run on the development machine (no device, no GPS, no Play services). They
// verify the pure, testable pieces of the concept by driving the FAKE repository
// and the ViewModel's state machine:
//   1. FakeLocationRepository returns a deterministic coordinate.
//   2. LocationViewModel transitions Idle -> Loading -> Success on a good fetch,
//      and -> Error when the repository throws or permission is denied.
//
// The ViewModel launches into viewModelScope (the Main dispatcher), which does not
// exist in a plain JVM test. Each coroutine test installs a test dispatcher that
// SHARES runTest's scheduler (via Dispatchers.setMain(StandardTestDispatcher(
// testScheduler))), so advanceUntilIdle() drives the ViewModel's coroutine and the
// fake's virtual delay() deterministically.
// =============================================================================
package com.example.locationservices

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit test suite for the LocationServices data + state layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)   // StandardTestDispatcher / advanceUntilIdle
class ExampleUnitTest {

    /** Sanity check kept from the template — confirms the test toolchain runs. */
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    /** The offline fake returns its fixed coordinate. */
    @Test
    fun fakeRepository_returnsFixedCoordinate() = runTest {
        val expected = LocationData(1.0, 2.0)
        val repo = FakeLocationRepository(fixed = expected)
        assertEquals(expected, repo.currentLocation())
    }

    /** A good fetch drives the ViewModel into Success carrying the coordinate. */
    @Test
    fun viewModel_fetch_emitsSuccess() = runTest {
        // Share THIS test's scheduler with Main so advanceUntilIdle controls the
        // ViewModel's viewModelScope coroutine (and the fake's virtual delay).
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val coordinate = LocationData(40.7128, -74.0060)
            val viewModel = LocationViewModel(FakeLocationRepository(fixed = coordinate))

            // Starts Idle (we do not fetch in init — we wait for permission).
            assertTrue(viewModel.uiState.value is LocationUiState.Idle)

            viewModel.fetchLocation()
            advanceUntilIdle()                   // run the launched coroutine + delay() to completion

            val state = viewModel.uiState.value
            assertTrue("expected Success but was $state", state is LocationUiState.Success)
            assertEquals(coordinate, (state as LocationUiState.Success).location)
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** When the repository throws, the ViewModel publishes an Error (never crashes). */
    @Test
    fun viewModel_fetch_emitsErrorOnFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val failing = object : LocationRepository {
                override suspend fun currentLocation(): LocationData =
                    throw IllegalStateException("no fix")
            }
            val viewModel = LocationViewModel(failing)

            viewModel.fetchLocation()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("expected Error but was $state", state is LocationUiState.Error)
            assertEquals("no fix", (state as LocationUiState.Error).message)
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** A denied permission is surfaced as an Error state, not a silent no-op. */
    @Test
    fun viewModel_permissionDenied_emitsError() {
        // Constructing the ViewModel launches nothing (no fetch in init), so this
        // synchronous check needs no Main dispatcher.
        val viewModel = LocationViewModel(FakeLocationRepository())
        viewModel.onPermissionDenied()
        assertTrue(viewModel.uiState.value is LocationUiState.Error)
    }
}
