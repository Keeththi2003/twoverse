package app.twoverse.feature.vault

import androidx.lifecycle.SavedStateHandle
import app.twoverse.core.common.ExpiryBadge
import app.twoverse.core.data.fake.FakeLocationRepository
import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.local.OursLock
import app.twoverse.core.data.settings.DefaultSettingsRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MemorySender
import app.twoverse.testing.InMemoryUserPreferences
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(VaultNow, ZoneId.of("Asia/Colombo"))
    private val memoryRepository = FakeMemoryRepository(clock)
    private val preferences = InMemoryUserPreferences()
    private val settingsRepository = DefaultSettingsRepository(FakeLocationRepository(), FakeProfileRepository(), preferences)
    private lateinit var oursLock: OursLock

    private fun TestScope.open(id: String, unlocked: Boolean = true): MemoryViewModel {
        oursLock = OursLock(backgroundScope)
        if (unlocked) oursLock.unlock()
        val viewModel = MemoryViewModel(
            SavedStateHandle(mapOf(MemoryIdKey to id)),
            memoryRepository,
            settingsRepository,
            oursLock,
            clock,
        )
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private fun MemoryViewModel.viewing() = uiState.value.content as MemoryContent.Viewing

    private fun storedMemory(id: String) = memoryRepository.memories.value.find { it.id == id }

    @Test
    fun showsTheMemoryInLocalTime() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("m", sentAgo = Duration.ofHours(2), expiresIn = Duration.ofDays(2))))

        val content = open("m").viewing()

        assertEquals(MemorySender.Partner, content.sender)
        assertEquals("Caption m", content.caption)
        assertEquals(LocalDateTime.of(2026, 9, 26, 15, 30), content.sentAt)
        assertEquals(ExpiryBadge.Days(2), content.expiryBadge)
    }

    @Test
    fun openingANewMemoryMarksItViewed() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("m")))

        open("m")

        assertEquals(VaultNow, storedMemory("m")?.viewedAt)
    }

    @Test
    fun theRecipientHidesAndTheSenderDeletes() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("received"), testMemory("sent", sender = MemorySender.Me)))

        assertEquals(MemoryRemoval.Hide, open("received").viewing().removal)
        assertEquals(MemoryRemoval.Delete, open("sent").viewing().removal)
    }

    @Test
    fun hidingAReceivedMemoryAfterConfirmingReturnsToTheVault() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("received")))
        val viewModel = open("received")

        viewModel.onRemoveRequested()
        runCurrent()
        assertTrue(viewModel.uiState.value.isRemoveDialogOpen)
        viewModel.onRemoveConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.isRemoved)
        assertNull(storedMemory("received"))
    }

    @Test
    fun deletingASentMemoryRemovesIt() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("sent", sender = MemorySender.Me)))
        val viewModel = open("sent")

        viewModel.onRemoveRequested()
        viewModel.onRemoveConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.isRemoved)
        assertNull(storedMemory("sent"))
    }

    @Test
    fun dismissingTheDialogKeepsTheMemory() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("sent", sender = MemorySender.Me)))
        val viewModel = open("sent")

        viewModel.onRemoveRequested()
        viewModel.onRemoveDismissed()
        runCurrent()

        assertFalse(viewModel.uiState.value.isRemoveDialogOpen)
        assertNotNull(storedMemory("sent"))
    }

    @Test
    fun aFailedDeleteShowsAnErrorAndKeepsTheMemory() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("sent", sender = MemorySender.Me)))
        val viewModel = open("sent")
        memoryRepository.failNextWith(DataError.Network)

        viewModel.onRemoveRequested()
        viewModel.onRemoveConfirmed()
        runCurrent()

        assertFalse(viewModel.uiState.value.isRemoved)
        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertNotNull(storedMemory("sent"))
    }

    @Test
    fun keepForeverOnlyWhenTheSenderAllowedIt() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(
            listOf(
                testMemory("keepable", expiresIn = Duration.ofDays(1), allowKeep = true),
                testMemory("not-allowed", expiresIn = Duration.ofDays(1)),
                testMemory("forever", allowKeep = true),
                testMemory("own", sender = MemorySender.Me, expiresIn = Duration.ofDays(1), allowKeep = true),
            ),
        )

        assertTrue(open("keepable").viewing().canKeepForever)
        assertFalse(open("not-allowed").viewing().canKeepForever)
        assertFalse(open("forever").viewing().canKeepForever)
        assertFalse(open("own").viewing().canKeepForever)
    }

    @Test
    fun keepingForeverRemovesTheExpiry() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("keepable", expiresIn = Duration.ofDays(1), allowKeep = true)))
        val viewModel = open("keepable")

        viewModel.onKeepForever()
        runCurrent()

        assertNull(viewModel.viewing().expiryBadge)
        assertFalse(viewModel.viewing().canKeepForever)
    }

    @Test
    fun expiredOrMissingMemoriesShowExpired() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("expired", sentAgo = Duration.ofDays(2), expiresIn = Duration.ZERO)))

        assertEquals(MemoryContent.Expired, open("expired").uiState.value.content)
        assertEquals(MemoryContent.Expired, open("missing").uiState.value.content)
    }

    @Test
    fun aLockedOursHidesTheMemoryUntilUnlocked() = runTest(mainDispatcherRule.testDispatcher) {
        memoryRepository.setMemories(listOf(testMemory("m")))
        val viewModel = open("m", unlocked = false)

        assertTrue(viewModel.uiState.value.isLocked)
        assertEquals(MemoryContent.Loading, viewModel.uiState.value.content)

        viewModel.onUnlocked()
        runCurrent()

        assertFalse(viewModel.uiState.value.isLocked)
        assertEquals("Caption m", viewModel.viewing().caption)
    }
}
