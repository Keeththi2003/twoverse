package app.twoverse.feature.compass

import app.twoverse.core.data.fake.FakeHeadingSource
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.sensors.CompassHeading
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.HeadingReading
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
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class CompassViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository = FakeLocationRepository()
    private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

    private fun TestScope.createViewModel(source: FakeHeadingSource): CompassViewModel {
        val viewModel = CompassViewModel(
            locationRepository,
            DefaultSettingsRepository(locationRepository, InMemoryUserPreferences()),
            CompassHeading(source, clock),
            clock,
        )
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.needleRotation.collect {} }
        runCurrent()
        return viewModel
    }

    private fun CompassViewModel.success() = uiState.value as CompassUiState.Success

    @Test
    fun needlePointsAtThePartnerAsThePhoneTurns() = runTest(mainDispatcherRule.testDispatcher) {
        val source = FakeHeadingSource()
        val viewModel = createViewModel(source)

        source.readings.emit(HeadingReading(magneticDegrees = 12.0, isAccurate = true))
        runCurrent()

        assertEquals(30f, viewModel.needleRotation.value ?: 0f, 0.2f)
        assertEquals(42, viewModel.success().bearingDegrees)
        assertFalse(viewModel.success().isPointingAtPartner)
    }

    @Test
    fun facingThePartnerIsDetected() = runTest(mainDispatcherRule.testDispatcher) {
        val source = FakeHeadingSource()
        val viewModel = createViewModel(source)

        source.readings.emit(HeadingReading(magneticDegrees = 40.0, isAccurate = true))
        runCurrent()

        assertTrue(viewModel.success().isPointingAtPartner)
    }

    @Test
    fun lowAccuracyAsksForCalibration() = runTest(mainDispatcherRule.testDispatcher) {
        val source = FakeHeadingSource()
        val viewModel = createViewModel(source)

        source.readings.emit(HeadingReading(magneticDegrees = 0.0, isAccurate = false))
        runCurrent()

        assertFalse(viewModel.success().isCalibrated)
    }

    @Test
    fun withoutASensorTheDirectionIsTextOnly() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(FakeHeadingSource(isAvailable = false))
        val state = viewModel.success()

        assertFalse(state.hasCompassSensor)
        assertFalse(state.showsNeedle)
        assertEquals(42, state.bearingDegrees)
    }

    @Test
    fun unavailablePartnerHidesTheNeedle() = runTest(mainDispatcherRule.testDispatcher) {
        locationRepository.partner.value = null
        val viewModel = createViewModel(FakeHeadingSource())

        assertFalse(viewModel.success().showsNeedle)
        assertNull(viewModel.needleRotation.value)
    }
}
