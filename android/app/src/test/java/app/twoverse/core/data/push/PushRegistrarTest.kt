package app.twoverse.core.data.push

import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.fake.FakePushRepository
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrarTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val pushRepository = FakePushRepository()
    private val profileRepository = FakeProfileRepository()
    private val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("Asia/Colombo"))

    private fun TestScope.registrar() = PushRegistrar(authRepository, pushRepository, profileRepository, clock, backgroundScope)

    @Test
    fun registersThisDeviceOnSignIn() = runTest(mainDispatcherRule.testDispatcher) {
        registrar().start()
        runCurrent()
        assertEquals(emptyList<String>(), pushRepository.calls)

        authRepository.signInWithEmail("you@example.com", "secret1")
        runCurrent()

        assertEquals(listOf("register:current"), pushRepository.calls)
        assertEquals(listOf("Asia/Colombo"), profileRepository.savedTimeZones)
    }

    @Test
    fun registersAgainAfterSigningInAgain() = runTest(mainDispatcherRule.testDispatcher) {
        registrar().start()
        authRepository.signInWithEmail("you@example.com", "secret1")
        runCurrent()
        authRepository.signOut()
        runCurrent()
        authRepository.signInWithGoogle("token", "nonce")
        runCurrent()

        assertEquals(listOf("register:current", "register:current"), pushRepository.calls)
    }

    @Test
    fun refreshedTokenIsSavedWhenSignedIn() = runTest(mainDispatcherRule.testDispatcher) {
        authRepository.signInWithEmail("you@example.com", "secret1")
        val registrar = registrar()

        registrar.onNewToken("fresh-token")
        runCurrent()

        assertEquals(listOf("register:fresh-token"), pushRepository.calls)
    }

    @Test
    fun refreshedTokenIsIgnoredWhenSignedOut() = runTest(mainDispatcherRule.testDispatcher) {
        val registrar = registrar()

        registrar.onNewToken("fresh-token")
        runCurrent()

        assertEquals(emptyList<String>(), pushRepository.calls)
    }
}
