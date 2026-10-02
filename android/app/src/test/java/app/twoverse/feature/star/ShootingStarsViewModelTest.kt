package app.twoverse.feature.star

import app.twoverse.core.data.fake.FakeShootingStarRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarStatus
import app.twoverse.testing.MainDispatcherRule
import app.twoverse.testing.testStar
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
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ShootingStarsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeShootingStarRepository()
    private val now = Instant.parse("2026-09-29T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Colombo"))

    private fun TestScope.createViewModel(): ShootingStarsViewModel {
        val viewModel = ShootingStarsViewModel(repository, clock)
        backgroundScope.launch(mainDispatcherRule.testDispatcher) { viewModel.uiState.collect {} }
        viewModel.refresh()
        runCurrent()
        return viewModel
    }

    @Test
    fun sentStarsShowTheirStatusNewestFirst() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(
            listOf(
                testStar(id = "seen", seenAt = now, createdAt = now.minusSeconds(300)),
                testStar(id = "scheduled", showAt = Instant.parse("2026-10-12T03:30:00Z"), createdAt = now.minusSeconds(100)),
                testStar(id = "waiting", createdAt = now.minusSeconds(200)),
            ),
        )

        val sent = createViewModel().uiState.value.sent

        assertEquals(listOf("scheduled", "waiting", "seen"), sent.map { it.id })
        assertEquals(listOf(StarStatus.Scheduled, StarStatus.Waiting, StarStatus.Seen), sent.map { it.status })
        assertEquals(listOf(true, true, false), sent.map { it.isEditable })
        assertEquals(LocalDateTime.of(2026, 10, 12, 9, 0), sent.first().time)
    }

    @Test
    fun theHeadlineIsTheTitleElseTheStartOfTheMessage() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(
            listOf(
                testStar(id = "title", content = StarContent(StarLayout.MessageOnly, title = "Good luck", message = "x")),
                testStar(id = "message", content = StarContent(StarLayout.MessageOnly, message = "\nFirst line\nSecond")),
                testStar(id = "photo", content = StarContent(StarLayout.FullPhoto), hasPhoto = true),
            ),
        )

        val headlines = createViewModel().uiState.value.sent.associate { it.id to it.headline }

        assertEquals("Good luck", headlines["title"])
        assertEquals("First line", headlines["message"])
        assertNull(headlines["photo"])
    }

    @Test
    fun receivedStarsCanBeViewedAgainButNotEdited() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(listOf(testStar(id = "from-her", seenAt = now)))

        val received = createViewModel().uiState.value.received

        assertEquals(listOf("from-her"), received.map { it.id })
        assertFalse(received.single().isEditable)
    }

    @Test
    fun deletingAsksFirstThenRemovesTheStar() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(listOf(testStar(id = "mine")))
        val viewModel = createViewModel()

        viewModel.onDelete("mine")
        runCurrent()
        assertEquals("mine", viewModel.uiState.value.pendingDeleteId)
        viewModel.onDeleteDismissed()
        runCurrent()
        assertNull(viewModel.uiState.value.pendingDeleteId)
        assertEquals(1, viewModel.uiState.value.sent.size)

        viewModel.onDelete("mine")
        viewModel.onDeleteConfirmed()
        runCurrent()

        assertTrue(viewModel.uiState.value.sent.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun aStarSeenInTheMeantimeCannotBeDeleted() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(listOf(testStar(id = "mine", seenAt = now)))
        val viewModel = createViewModel()

        viewModel.onDelete("mine")
        viewModel.onDeleteConfirmed()
        runCurrent()

        assertEquals(DataError.StarUnavailable, viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.sent.size)
    }

    @Test
    fun aFailedLoadCanBeRetried() = runTest(mainDispatcherRule.testDispatcher) {
        repository.failNextWith(DataError.Network)
        val viewModel = createViewModel()
        assertEquals(DataError.Network, viewModel.uiState.value.loadError)

        viewModel.refresh()
        runCurrent()

        assertNull(viewModel.uiState.value.loadError)
    }
}
