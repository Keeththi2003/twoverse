package app.twoverse.feature.home

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.data.fake.FakeLocationPermissionChecker
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakeOrbitRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.fake.FakeReunionRepository
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.LocationPermissionStatus
import app.twoverse.core.model.Meetup
import app.twoverse.core.model.ReunionPlan
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository = FakeLocationRepository()
    private val preferences = InMemoryUserPreferences()
    private val permissions = FakeLocationPermissionChecker()
    private val reunionRepository = FakeReunionRepository()
    private val orbitRepository = FakeOrbitRepository()
    private var clock: Clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
    private lateinit var viewModel: HomeViewModel

    private fun TestScope.state(): HomeUiState.Success {
        viewModel = HomeViewModel(
            locationRepository = locationRepository,
            settingsRepository = DefaultSettingsRepository(locationRepository, FakeProfileRepository(), preferences),
            reunionRepository = reunionRepository,
            memoryRepository = FakeMemoryRepository(Clock.systemUTC()),
            orbitRepository = orbitRepository,
            permissions = permissions,
            preferences = preferences,
            clock = clock,
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
    fun theDistanceCardShowsHerDirectionAndBearing() = runTest(mainDispatcherRule.testDispatcher) {
        val state = state()

        assertEquals(CompassDirection.NorthEast, state.partnerDirection)
        assertEquals(42, state.partnerBearing)
    }

    @Test
    fun withoutHerLocationThereIsNoDirection() = runTest(mainDispatcherRule.testDispatcher) {
        locationRepository.partner.value = null
        val state = state()

        assertNull(state.partnerDirection)
        assertNull(state.partnerBearing)
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

    // Our Orbit (FR-ORB-2, FR-ORB-9, FR-ORB-10)

    private val meetupDay = Instant.parse("2026-10-10T08:00:00Z")

    private suspend fun passedReunion() {
        clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC)
        reunionRepository.saveReunion(ReunionPlan(meetAt = meetupDay, hasTime = true, place = "Kandy", note = null))
    }

    @Test
    fun withoutTogetherSinceHomeOffersToSetIt() = runTest(mainDispatcherRule.testDispatcher) {
        val state = state()

        assertTrue(state.askTogetherSince)
        assertNull(state.orbit)
    }

    @Test
    fun showsDaysTogetherAndTimesMet() = runTest(mainDispatcherRule.testDispatcher) {
        clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC)
        orbitRepository.setTogetherSince(LocalDate.of(2026, 10, 1))
        orbitRepository.setMeetups(
            listOf(
                Meetup("a", LocalDate.of(2026, 10, 3), null, null, null),
                Meetup("b", LocalDate.of(2026, 10, 7), null, null, null),
            ),
        )

        val state = state()

        assertFalse(state.askTogetherSince)
        assertEquals(HomeOrbit(totalDays = 12, timesMet = 2), state.orbit)
    }

    @Test
    fun aPassedReunionAsksWhetherYouMet() = runTest(mainDispatcherRule.testDispatcher) {
        passedReunion()

        assertEquals(LocalDate.of(2026, 10, 10), state().meetupQuestion)
    }

    @Test
    fun anUpcomingReunionAsksNothing() = runTest(mainDispatcherRule.testDispatcher) {
        clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC)
        reunionRepository.saveReunion(ReunionPlan(meetAt = meetupDay, hasTime = true, place = null, note = null))

        assertNull(state().meetupQuestion)
    }

    @Test
    fun aReunionRecordedAsAMeetupIsNotAskedAgain() = runTest(mainDispatcherRule.testDispatcher) {
        passedReunion()
        orbitRepository.setMeetups(listOf(Meetup("m", LocalDate.of(2026, 10, 10), null, "Kandy", null, fromReunionAt = meetupDay)))

        assertNull(state().meetupQuestion)
    }

    @Test
    fun answeringNoDismissesTheQuestionOnThisDevice() = runTest(mainDispatcherRule.testDispatcher) {
        passedReunion()
        state()

        viewModel.onMeetupQuestionDismissed()
        runCurrent()

        assertEquals(meetupDay, preferences.dismissedMeetupQuestion.value)
        assertNull((viewModel.uiState.value as HomeUiState.Success).meetupQuestion)
    }
}
