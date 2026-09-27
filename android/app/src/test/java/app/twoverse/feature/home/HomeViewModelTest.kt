package app.twoverse.feature.home

import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.data.fake.FakeLocationPermissionChecker
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPermissionStatus
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository = FakeLocationRepository()
    private val preferences = InMemoryUserPreferences()
    private val permissions = FakeLocationPermissionChecker()

    private fun TestScope.state(): HomeUiState.Success {
        val viewModel = HomeViewModel(
            locationRepository = locationRepository,
            settingsRepository = DefaultSettingsRepository(locationRepository, preferences),
            reunionRepository = FakeReunionRepository(),
            memoryRepository = FakeMemoryRepository(),
            permissions = permissions,
            clock = Clock.fixed(Instant.now(), ZoneOffset.UTC),
        )
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel.uiState.value as HomeUiState.Success
    }

    @Test
    fun sharedLocationsShowLiveDistance() = runTest(mainDispatcherRule.testDispatcher) {
        val state = state()

        assertEquals(SharingStatus.On, state.sharingStatus)
        assertEquals("94.6", state.distance)
        assertEquals(LocationFreshness.Live, state.freshness)
    }

    @Test
    fun distanceUsesTheChosenUnit() = runTest(mainDispatcherRule.testDispatcher) {
        preferences.setDistanceUnit(DistanceUnit.Miles)

        assertEquals("58.8", state().distance)
    }

    @Test
    fun sharingOffShowsDistanceUnavailable() = runTest(mainDispatcherRule.testDispatcher) {
        locationRepository.setSharingEnabled(false)
        val state = state()

        assertEquals(SharingStatus.Off, state.sharingStatus)
        assertNull(state.distance)
        assertEquals(LocationUnavailableReason.SharingOff, state.unavailableReason)
    }

    @Test
    fun partnerNotSharingShowsDistanceUnavailable() = runTest(mainDispatcherRule.testDispatcher) {
        locationRepository.partner.value = null
        val state = state()

        assertNull(state.distance)
        assertEquals(LocationFreshness.Unavailable, state.freshness)
        assertEquals(LocationUnavailableReason.PartnerUnavailable, state.unavailableReason)
    }

    @Test
    fun revokedPermissionAsksToAllowLocation() = runTest(mainDispatcherRule.testDispatcher) {
        permissions.current = LocationPermissionStatus(foreground = false, background = false)

        assertEquals(SharingStatus.PermissionNeeded, state().sharingStatus)
    }
}
