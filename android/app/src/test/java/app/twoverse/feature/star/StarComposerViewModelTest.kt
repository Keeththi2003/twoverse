package app.twoverse.feature.star

import androidx.lifecycle.SavedStateHandle
import app.twoverse.core.data.fake.FakeAuthRepository
import app.twoverse.core.data.fake.FakeProfileRepository
import app.twoverse.core.data.fake.FakeShootingStarRepository
import app.twoverse.core.data.sample.SampleData
import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarDraftProblem
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoChange
import app.twoverse.core.model.StarPhotoFit
import app.twoverse.testing.MainDispatcherRule
import app.twoverse.testing.testStar
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StarComposerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeShootingStarRepository()
    private val authRepository = FakeAuthRepository()
    private val profileRepository = FakeProfileRepository()
    private val now = Instant.parse("2026-09-29T09:00:00Z")
    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(now, zone)
    private val today = LocalDate.of(2026, 9, 29)

    private suspend fun TestScope.createViewModel(starId: String? = null): StarComposerViewModel {
        authRepository.signInWithGoogle("token", "nonce")
        val viewModel = StarComposerViewModel(
            SavedStateHandle(mapOf(StarIdKey to starId)),
            repository,
            authRepository,
            profileRepository,
            clock,
        )
        runCurrent()
        return viewModel
    }

    private val StarComposerViewModel.state get() = uiState.value

    // A new star

    @Test
    fun aNewStarIsSignedWithMyName() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.isLoading)
        assertFalse(viewModel.state.isEditing)
        assertEquals(SampleData.me.displayName, viewModel.state.signature)
        assertEquals(StarLayout.PhotoMessage, viewModel.state.layout)
        assertEquals(StarSchedule.NextOpen, viewModel.state.schedule)
    }

    @Test
    fun aTemplateFillsTheEyebrowAndTitleButKeepsTheRest() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onMessageChange("My words")

        viewModel.onTemplateSelected(StarTemplate.Birthday, StarTemplateText(eyebrow = "For you", title = "Happy Birthday"))

        assertEquals("For you", viewModel.state.eyebrow)
        assertEquals("Happy Birthday", viewModel.state.title)
        assertEquals("My words", viewModel.state.message)
        assertEquals(SampleData.me.displayName, viewModel.state.signature)
        assertEquals(StarTemplate.Birthday, viewModel.state.template)

        viewModel.onTemplateSelected(StarTemplate.Blank, StarTemplateText(eyebrow = "", title = ""))
        assertEquals("", viewModel.state.eyebrow)
        assertEquals("", viewModel.state.title)
        assertEquals("My words", viewModel.state.message)
    }

    @Test
    fun textIsLimitedToWhatTheBackendAccepts() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onMessageChange("x".repeat(StarContent.MaxMessageLength + 5))
        viewModel.onTitleChange("t".repeat(StarContent.MaxTitleLength + 5))
        viewModel.onEyebrowChange("e".repeat(StarContent.MaxEyebrowLength + 5))
        viewModel.onSignatureChange("s".repeat(StarContent.MaxSignatureLength + 5))

        assertEquals(StarContent.MaxMessageLength, viewModel.state.message.length)
        assertEquals(StarContent.MaxTitleLength, viewModel.state.title.length)
        assertEquals(StarContent.MaxEyebrowLength, viewModel.state.eyebrow.length)
        assertEquals(StarContent.MaxSignatureLength, viewModel.state.signature.length)
    }

    @Test
    fun sendsForTheNextOpenWithOnlyTheLayoutsFields() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onEyebrowChange("For you")
        viewModel.onTitleChange("  Happy Birthday ")
        viewModel.onPhotoPicked("content://photo")
        viewModel.onLayoutSelected(StarLayout.FullPhoto)
        viewModel.onPhotoFitSelected(StarPhotoFit.Fit)

        viewModel.onSend()
        runCurrent()

        assertTrue(viewModel.state.isSaved)
        val (id, draft) = repository.savedDrafts.single()
        assertNull(id)
        assertNull(draft.showAt)
        assertEquals(StarLayout.FullPhoto, draft.content.layout)
        assertEquals("Happy Birthday", draft.content.title)
        assertNull("Full photo has no eyebrow", draft.content.eyebrow)
        assertNull("Full photo has no signature", draft.content.signature)
        assertEquals(StarPhotoFit.Fit, draft.content.photoFit)
        assertEquals(StarPhotoChange.Replace("content://photo"), draft.photo)
    }

    @Test
    fun aFullPhotoStarWithoutAPhotoIsNotSent() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onLayoutSelected(StarLayout.FullPhoto)
        viewModel.onTitleChange("Us")

        viewModel.onSend()
        runCurrent()

        assertEquals(StarDraftProblem.PhotoRequired, viewModel.state.problem)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test
    fun aStarWithNothingToShowIsNotSent() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onLayoutSelected(StarLayout.MessageOnly)

        viewModel.onSend()
        runCurrent()

        assertEquals(StarDraftProblem.NothingToShow, viewModel.state.problem)
        assertTrue(repository.savedDrafts.isEmpty())

        viewModel.onMessageChange("Hi")
        assertNull("editing clears the problem", viewModel.state.problem)
    }

    @Test
    fun thePreviewShowsExactlyWhatWillBeSent() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange(" Hi ")
        viewModel.onEyebrowChange("  ")
        viewModel.onPhotoPicked("content://photo")
        viewModel.onLayoutSelected(StarLayout.MessageOnly)

        val display = viewModel.state.display
        assertEquals("Hi", display.content.title)
        assertNull(display.content.eyebrow)
        assertNull("Message only shows no photo", display.photo)

        viewModel.onOpenPreview()
        viewModel.onPreviewDarkChange(true)
        assertTrue(viewModel.state.isPreviewOpen)
        assertEquals(true, viewModel.state.isPreviewDark)
        viewModel.onClosePreview()
        assertFalse(viewModel.state.isPreviewOpen)
    }

    // Scheduling (FR-STAR-7)

    @Test
    fun choosingADateAsksForTheTimeAndSavesItInUtc() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange("Happy Birthday")

        viewModel.onOpenPicker(StarPicker.Date)
        viewModel.onDateSelected(LocalDate.of(2026, 10, 12))
        assertEquals(StarPicker.Time, viewModel.state.openPicker)
        viewModel.onTimeSelected(LocalTime.of(0, 0))
        assertEquals(StarSchedule.At(LocalDate.of(2026, 10, 12), LocalTime.MIDNIGHT), viewModel.state.schedule)

        viewModel.onSend()
        runCurrent()

        // Midnight in Colombo (UTC+5:30) is 18:30 UTC the day before.
        assertEquals(Instant.parse("2026-10-11T18:30:00Z"), repository.savedDrafts.single().second.showAt)
    }

    @Test
    fun aTimeThatHasPassedIsNotAccepted() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onTitleChange("Hi")
        viewModel.onDateSelected(today)
        // 09:00 UTC is 14:30 in Colombo, so 14:00 today has passed.
        viewModel.onTimeSelected(LocalTime.of(14, 0))

        viewModel.onSend()
        runCurrent()

        assertEquals(StarDraftProblem.ShowAtInPast, viewModel.state.problem)
        assertTrue(repository.savedDrafts.isEmpty())
    }

    @Test
    fun pastDatesCannotBeChosenAndNextOpenClearsTheTime() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onDateSelected(today.minusDays(1))
        assertEquals(StarSchedule.NextOpen, viewModel.state.schedule)

        viewModel.onDateSelected(today.plusDays(1))
        viewModel.onTimeSelected(LocalTime.of(20, 0))
        viewModel.onShowNextOpen()
        assertEquals(StarSchedule.NextOpen, viewModel.state.schedule)
    }

    @Test
    fun scheduleConvertsLocalTimeToUtc() {
        assertNull(StarSchedule.NextOpen.showAt(zone))
        assertEquals(
            Instant.parse("2026-10-12T03:30:00Z"),
            StarSchedule.At(LocalDate.of(2026, 10, 12), LocalTime.of(9, 0)).showAt(zone),
        )
    }

    // Editing (FR-STAR-9)

    @Test
    fun editingLoadsTheStarAndSavesOverIt() = runTest(mainDispatcherRule.testDispatcher) {
        val showAt = Instant.parse("2026-10-12T03:30:00Z")
        repository.setSent(
            listOf(
                testStar(
                    id = "mine",
                    content = StarContent(StarLayout.MessageOnly, eyebrow = "For you", title = "Good luck", signature = null),
                    showAt = showAt,
                ),
            ),
        )
        val viewModel = createViewModel(starId = "mine")

        assertTrue(viewModel.state.isEditing)
        assertEquals(StarLayout.MessageOnly, viewModel.state.layout)
        assertEquals("Good luck", viewModel.state.title)
        assertEquals("", viewModel.state.signature)
        assertEquals(StarSchedule.At(LocalDate.of(2026, 10, 12), LocalTime.of(9, 0)), viewModel.state.schedule)

        viewModel.onTitleChange("Good luck today")
        viewModel.onSend()
        runCurrent()

        val (id, draft) = repository.savedDrafts.single()
        assertEquals("mine", id)
        assertEquals("Good luck today", draft.content.title)
        assertEquals(showAt, draft.showAt)
        assertEquals(StarPhotoChange.Keep, draft.photo)
    }

    @Test
    fun editingAStarThatIsAlreadyWaitingKeepsItForTheNextOpen() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(listOf(testStar(id = "mine", showAt = now.minusSeconds(3_600))))

        val viewModel = createViewModel(starId = "mine")

        assertEquals(StarSchedule.NextOpen, viewModel.state.schedule)
    }

    @Test
    fun aSeenStarCannotBeEdited() = runTest(mainDispatcherRule.testDispatcher) {
        repository.setSent(listOf(testStar(id = "mine", seenAt = now.minusSeconds(60))))

        val viewModel = createViewModel(starId = "mine")

        assertEquals(DataError.StarUnavailable, viewModel.state.loadError)
        assertFalse(viewModel.state.canSend)
    }

    @Test
    fun aFailedSendKeepsEverythingForARetry() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onMessageChange("Hi")
        repository.failNextWith(DataError.Network)

        viewModel.onSend()
        runCurrent()

        assertEquals(DataError.Network, viewModel.state.error)
        assertFalse(viewModel.state.isSaved)
        assertFalse(viewModel.state.isSaving)
        assertEquals("Hi", viewModel.state.message)
    }
}
