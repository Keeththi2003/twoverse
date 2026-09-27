package app.twoverse.core.data.settings

import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import app.twoverse.core.model.DevicePosition
import app.twoverse.core.model.LocationPrecision
import app.twoverse.testing.InMemoryUserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultSettingsRepositoryTest {

    private val locationRepository = FakeLocationRepository()
    private val preferences = InMemoryUserPreferences()
    private val repository = DefaultSettingsRepository(locationRepository, preferences)

    @Test
    fun sharingSettingsComeFromTheLocationRow() = runTest {
        repository.setLocationPrecision(LocationPrecision.Precise)
        repository.setShareLocation(false)

        val settings = repository.settings.first()
        assertEquals(false, settings.shareLocation)
        assertEquals(LocationPrecision.Precise, settings.locationPrecision)
    }

    @Test
    fun turningSharingOffRemovesTheOwnPosition() = runTest {
        repository.setShareLocation(false)

        assertNull(locationRepository.myLocation.value)
    }

    @Test
    fun uploadsAreRoundedForApproximatePrecision() = runTest {
        locationRepository.upload(DevicePosition(6.927123, 79.861244, 10f))

        val stored = locationRepository.myLocation.value
        assertEquals(6.93, stored?.latitude ?: 0.0, 0.0)
        assertEquals(79.86, stored?.longitude ?: 0.0, 0.0)
    }

    @Test
    fun serverFailuresAreReturned() = runTest {
        locationRepository.failNextWith = DataError.Network

        assertEquals(DataResult.Failure(DataError.Network), repository.setShareLocation(true))
    }

    @Test
    fun lockOursIsADevicePreference() = runTest {
        repository.setLockOurs(false)

        assertEquals(false, repository.settings.first().lockOurs)
        assertEquals(false, preferences.lockOurs.value)
    }
}
