package app.twoverse.core.data.settings

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.model.AppearanceMode
import app.twoverse.core.model.DataError
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.ProfileSettings
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSettingsSyncTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val profileRepository = FakeProfileRepository()
    private val preferences = InMemoryUserPreferences()

    private fun TestScope.startSync() {
        ProfileSettingsSync(authRepository, profileRepository, preferences, backgroundScope).start()
        runCurrent()
    }

    @Test
    fun signingInUsesTheSettingsSavedOnTheProfile() = runTest(mainDispatcherRule.testDispatcher) {
        profileRepository.serverSettings = ProfileSettings(
            lockOurs = false,
            distanceUnit = DistanceUnit.Miles,
            appearance = AppearanceMode.Dark,
        )
        startSync()

        authRepository.signInWithEmail("you@example.com", "secret1")
        runCurrent()

        assertEquals(false, preferences.lockOurs.value)
        assertEquals(DistanceUnit.Miles, preferences.distanceUnit.value)
        assertEquals(AppearanceMode.Dark, preferences.appearance.value)
    }

    @Test
    fun offlineSignInKeepsTheDeviceSettings() = runTest(mainDispatcherRule.testDispatcher) {
        preferences.setDistanceUnit(DistanceUnit.Miles)
        profileRepository.failNextWith(DataError.Network)
        startSync()

        authRepository.signInWithEmail("you@example.com", "secret1")
        runCurrent()

        assertEquals(DistanceUnit.Miles, preferences.distanceUnit.value)
    }

    @Test
    fun loggingOutLocksOursAgain() = runTest(mainDispatcherRule.testDispatcher) {
        profileRepository.serverSettings = profileRepository.serverSettings.copy(lockOurs = false)
        startSync()
        authRepository.signInWithEmail("you@example.com", "secret1")
        runCurrent()

        authRepository.signOut()
        runCurrent()

        assertEquals(true, preferences.lockOurs.value)
    }
}
