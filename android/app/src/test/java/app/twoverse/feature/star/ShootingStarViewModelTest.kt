package app.twoverse.feature.star

import androidx.lifecycle.SavedStateHandle
import app.twoverse.core.data.fake.FakeShootingStarRepository
import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarLayout
import app.twoverse.testing.MainDispatcherRule
import app.twoverse.testing.testStar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ShootingStarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeShootingStarRepository()
    private val now = Instant.parse("2026-09-29T09:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Asia/Colombo"))

    private fun createViewModel(starId: String? = null) =
        ShootingStarViewModel(SavedStateHandle(mapOf(StarIdKey to starId)), repository, clock)

    private val ShootingStarViewModel.showing get() = uiState.value as ShootingStarUiState.Showing

    @Test
    fun withNothingWaitingTheAppContinues() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(listOf(testStar(seenAt = now), testStar(id = "later", showAt = now.plusSeconds(60))))

        val viewModel = createViewModel()
        runCurrent()

        assertEquals(ShootingStarUiState.Done, viewModel.uiState.value)
    }

    @Test
    fun waitingStarsAreShownOneAfterAnotherOldestFirstAndMarkedSeen() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(
            listOf(
                testStar(id = "newer", createdAt = now.minusSeconds(60)),
                testStar(id = "older", createdAt = now.minusSeconds(3_600)),
            ),
        )
        val viewModel = createViewModel()
        runCurrent()

        assertEquals("older", viewModel.showing.starId)
        assertTrue(viewModel.showing.hasNext)

        viewModel.onEnter()
        runCurrent()
        assertEquals("newer", viewModel.showing.starId)
        assertEquals(false, viewModel.showing.hasNext)

        viewModel.onEnter()
        runCurrent()
        assertEquals(ShootingStarUiState.Done, viewModel.uiState.value)
        assertEquals(listOf("older", "newer"), repository.markedSeen)
    }

    @Test
    fun showsThePhotoAndHidesEmptyFields() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(
            listOf(testStar(content = StarContent(StarLayout.PhotoMessage, eyebrow = " ", title = "Hi", signature = ""), hasPhoto = true)),
        )
        repository.photoUrls["star-1"] = "https://signed/photo"

        val viewModel = createViewModel()
        runCurrent()

        val display = viewModel.showing.display
        assertEquals("https://signed/photo", display.photo)
        assertEquals("Hi", display.content.title)
        assertNull(display.content.eyebrow)
        assertNull(display.content.signature)
    }

    @Test
    fun aStarThatCannotLoadIsSkippedAndStaysUnseen() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(
            listOf(
                testStar(id = "offline", createdAt = now.minusSeconds(3_600)),
                testStar(id = "fine", createdAt = now.minusSeconds(60)),
            ),
        )
        repository.failNextWith(DataError.Network)

        val viewModel = createViewModel()
        runCurrent()

        assertEquals("fine", viewModel.showing.starId)
        viewModel.onEnter()
        runCurrent()
        assertEquals(listOf("fine"), repository.markedSeen)
    }

    @Test
    fun enteringTwiceQuicklyMovesOnOnlyOnce() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(listOf(testStar(id = "a", createdAt = now.minusSeconds(60)), testStar(id = "b", createdAt = now)))
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onEnter()
        viewModel.onEnter()
        runCurrent()

        assertEquals("b", viewModel.showing.starId)
        assertEquals(listOf("a"), repository.markedSeen)
    }

    @Test
    fun aSeenStarCanBeViewedAgainWithoutChangingIt() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setReceived(listOf(testStar(id = "seen", seenAt = now.minusSeconds(86_400))))

        val viewModel = createViewModel(starId = "seen")
        runCurrent()
        assertEquals("seen", viewModel.showing.starId)

        viewModel.onEnter()
        runCurrent()
        assertEquals(ShootingStarUiState.Done, viewModel.uiState.value)
        assertTrue(repository.markedSeen.isEmpty())
    }
}
