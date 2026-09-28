package app.twoverse.feature.vault

import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.local.OursLock
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.MemorySender
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(VaultNow, ZoneOffset.UTC)
    private val memoryRepository = FakeMemoryRepository(clock)
    private val preferences = InMemoryUserPreferences()
    private val settingsRepository = DefaultSettingsRepository(FakeLocationRepository(), FakeProfileRepository(), preferences)

    private fun TestScope.createViewModel(): VaultViewModel {
        val viewModel = VaultViewModel(memoryRepository, settingsRepository, OursLock(backgroundScope), clock)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun TestScope.unlockedState(): VaultUiState.Success {
        val viewModel = createViewModel()
        viewModel.onUnlocked()
        runCurrent()
        return viewModel.uiState.value as VaultUiState.Success
    }

    @Test
    fun oursStaysLockedUntilTheUserConfirmsItsThem() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        assertEquals(VaultUiState.Locked, viewModel.uiState.value)

        viewModel.onUnlocked()
        runCurrent()

        assertEquals(VaultUiState.Success::class, viewModel.uiState.value::class)
    }

    @Test
    fun withLockOursOffOursOpensDirectly() = runTest(mainDispatcherRule.testDispatcher) {
        preferences.setLockOurs(false)

        assertEquals(VaultUiState.Success::class, createViewModel().uiState.value::class)
    }

    @Test
    fun showsUnexpiredMemoriesNewestFirstWithCount() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(
            listOf(
                testMemory("old", sentAgo = Duration.ofDays(3)),
                testMemory("expired", sentAgo = Duration.ofDays(2), expiresIn = Duration.ofMinutes(-1)),
                testMemory("new", sentAgo = Duration.ofMinutes(5)),
            ),
        )

        val state = unlockedState()

        assertEquals(2, state.totalCount)
        assertEquals(listOf("new", "old"), state.tiles.map { it.id })
    }

    @Test
    fun onlyUnopenedMemoriesFromThePartnerAreNew() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(
            listOf(
                testMemory("received", sentAgo = Duration.ofMinutes(1)),
                testMemory("opened", sentAgo = Duration.ofMinutes(2), viewed = true),
                testMemory("sent", sender = MemorySender.Me, sentAgo = Duration.ofMinutes(3)),
            ),
        )

        val tiles = unlockedState().tiles

        assertEquals(listOf(true, false, false), tiles.map { it.isNew })
    }

    @Test
    fun temporaryMemoriesShowTimeLeft() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(
            listOf(
                testMemory("day", sentAgo = Duration.ofMinutes(1), expiresIn = Duration.ofHours(24)),
                testMemory("week", sentAgo = Duration.ofMinutes(2), expiresIn = Duration.ofDays(7)),
                testMemory("forever", sentAgo = Duration.ofMinutes(3)),
            ),
        )

        val badges = unlockedState().tiles.map { it.expiryBadge }

        assertEquals(listOf(ExpiryBadge.Hours(24), ExpiryBadge.Days(7), null), badges)
    }

    @Test
    fun filtersKeepTheTotalCount() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(
            listOf(
                testMemory("received", sentAgo = Duration.ofMinutes(1), expiresIn = Duration.ofDays(1)),
                testMemory("sent", sender = MemorySender.Me, sentAgo = Duration.ofMinutes(2)),
            ),
        )
        val viewModel = createViewModel()
        viewModel.onUnlocked()

        viewModel.onFilterSelected(VaultFilter.FromMe)
        runCurrent()
        val fromMe = viewModel.uiState.value as VaultUiState.Success
        viewModel.onFilterSelected(VaultFilter.Expiring)
        runCurrent()
        val expiring = viewModel.uiState.value as VaultUiState.Success

        assertEquals(listOf("sent"), fromMe.tiles.map { it.id })
        assertEquals(listOf("received"), expiring.tiles.map { it.id })
        assertEquals(2, expiring.totalCount)
    }

    @Test
    fun emptyVaultHasNoTiles() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(emptyList())

        val state = unlockedState()

        assertEquals(0, state.totalCount)
        assertEquals(emptyList<VaultTile>(), state.tiles)
    }
}
