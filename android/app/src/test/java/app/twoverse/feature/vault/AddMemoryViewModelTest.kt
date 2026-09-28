package app.twoverse.feature.vault

import app.twoverse.core.data.fake.FakeMemoryRepository
import app.twoverse.core.data.fake.FakePushRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.MemoryDraft
import app.twoverse.core.model.MemoryExpiry
import app.twoverse.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AddMemoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val memoryRepository = FakeMemoryRepository(Clock.fixed(VaultNow, ZoneOffset.UTC))
    private val pushRepository = FakePushRepository()
    private val viewModel = AddMemoryViewModel(memoryRepository, pushRepository)

    @Test
    fun cannotSendWithoutAPhoto() {
        assertFalse(viewModel.uiState.value.canSend)

        viewModel.onPhotoPicked("content://photo")

        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun sendingUploadsTheDraftAndNotifiesThePartner() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onPhotoPicked("content://photo")
        viewModel.onCaptionChange("  Sunset  ")
        viewModel.onExpirySelected(MemoryExpiry.Days7)
        viewModel.onAllowKeepChange(true)

        viewModel.onSend()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isSent)
        assertFalse(state.isSending)
        assertEquals(1f, state.uploadProgress)
        assertEquals(
            listOf(MemoryDraft(photoUri = "content://photo", caption = "Sunset", expiry = MemoryExpiry.Days7, allowKeep = true)),
            memoryRepository.sentDrafts,
        )
        assertEquals(listOf("send:NewMemory"), pushRepository.calls)
    }

    @Test
    fun keepingForeverIsOnlySentForTemporaryMemories() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onPhotoPicked("content://photo")
        viewModel.onAllowKeepChange(true)

        viewModel.onSend()
        runCurrent()

        assertEquals(null, memoryRepository.sentDrafts.single().caption)
        assertFalse(memoryRepository.sentDrafts.single().allowKeep)
    }

    @Test
    fun aFailedUploadKeepsEverythingForARetry() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel.onPhotoPicked("content://photo")
        viewModel.onCaptionChange("Sunset")
        memoryRepository.failNextWith(DataError.Network)

        viewModel.onSend()
        runCurrent()

        val failed = viewModel.uiState.value
        assertTrue(failed.sendFailed)
        assertFalse(failed.isSent)
        assertEquals("Sunset", failed.caption)
        assertEquals("content://photo", failed.photoUri)
        assertTrue(failed.canSend)
        assertEquals(emptyList<String>(), pushRepository.calls)

        viewModel.onSend()
        runCurrent()

        assertTrue(viewModel.uiState.value.isSent)
        assertEquals("Sunset", memoryRepository.sentDrafts.single().caption)
    }

    @Test
    fun captionIsLimitedTo500Characters() {
        viewModel.onCaptionChange("a".repeat(600))

        assertEquals(500, viewModel.uiState.value.caption.length)
    }
}
