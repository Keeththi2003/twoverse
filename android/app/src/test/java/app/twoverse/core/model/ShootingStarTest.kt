package app.twoverse.core.model

import app.twoverse.testing.testStar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ShootingStarTest {

    private val now = Instant.parse("2026-09-29T09:00:00Z")

    // Field visibility (FR-STAR-4)

    @Test
    fun photoAndMessageShowsEveryTextFieldAndAnOptionalPhoto() {
        assertEquals(
            setOf(StarField.Eyebrow, StarField.Title, StarField.Message, StarField.Signature, StarField.Photo),
            StarLayout.PhotoMessage.fields,
        )
    }

    @Test
    fun messageOnlyHasNoPhoto() {
        assertFalse(StarLayout.MessageOnly.shows(StarField.Photo))
        assertFalse(StarLayout.MessageOnly.shows(StarField.PhotoFit))
        assertTrue(StarLayout.MessageOnly.shows(StarField.Signature))
    }

    @Test
    fun fullPhotoShowsThePhotoItsFitAndAShortTextOnly() {
        assertEquals(
            setOf(StarField.Photo, StarField.PhotoFit, StarField.Title, StarField.Message),
            StarLayout.FullPhoto.fields,
        )
    }

    @Test
    fun emptyFieldsAreRemovedSoTheViewerHidesThem() {
        val content = StarContent(
            layout = StarLayout.PhotoMessage,
            eyebrow = "  ",
            title = "",
            message = "  Happy birthday \n",
            signature = null,
        ).normalized()

        assertNull(content.eyebrow)
        assertNull(content.title)
        assertEquals("Happy birthday", content.message)
        assertNull(content.signature)
    }

    @Test
    fun fieldsTheLayoutHidesAreNotSaved() {
        val content = StarContent(
            layout = StarLayout.FullPhoto,
            eyebrow = "For you",
            title = "Us",
            signature = "Me",
            photoFit = StarPhotoFit.Fit,
        ).normalized()

        assertNull(content.eyebrow)
        assertNull(content.signature)
        assertEquals("Us", content.title)
        assertEquals(StarPhotoFit.Fit, content.photoFit)
        assertEquals(StarPhotoFit.Fill, StarContent(StarLayout.MessageOnly, photoFit = StarPhotoFit.Fit).normalized().photoFit)
    }

    // Validation (FR-STAR-5, FR-STAR-7)

    @Test
    fun aFullPhotoStarNeedsAPhoto() {
        val content = StarContent(StarLayout.FullPhoto, title = "Us").normalized()

        assertEquals(StarDraftProblem.PhotoRequired, validateStar(content, hasPhoto = false, showAt = null, now = now))
        assertNull(validateStar(content, hasPhoto = true, showAt = null, now = now))
    }

    @Test
    fun aStarMustShowATitleMessageOrPhoto() {
        val onlyEyebrowAndSignature = StarContent(StarLayout.MessageOnly, eyebrow = "For you", signature = "Me").normalized()
        val onlyPhoto = StarContent(StarLayout.PhotoMessage).normalized()
        val onlyMessage = StarContent(StarLayout.MessageOnly, message = "Hi").normalized()

        assertEquals(StarDraftProblem.NothingToShow, validateStar(onlyEyebrowAndSignature, hasPhoto = false, showAt = null, now = now))
        assertNull(validateStar(onlyPhoto, hasPhoto = true, showAt = null, now = now))
        assertNull(validateStar(onlyMessage, hasPhoto = false, showAt = null, now = now))
    }

    @Test
    fun aPhotoTheLayoutDoesNotShowDoesNotCount() {
        val content = StarContent(StarLayout.MessageOnly, eyebrow = "For you").normalized()

        assertEquals(StarDraftProblem.NothingToShow, validateStar(content, hasPhoto = true, showAt = null, now = now))
    }

    @Test
    fun aChosenTimeMustBeInTheFuture() {
        val content = StarContent(StarLayout.MessageOnly, title = "Hi").normalized()

        assertEquals(StarDraftProblem.ShowAtInPast, validateStar(content, hasPhoto = false, showAt = now.minusSeconds(60), now = now))
        assertEquals(StarDraftProblem.ShowAtInPast, validateStar(content, hasPhoto = false, showAt = now, now = now))
        assertNull(validateStar(content, hasPhoto = false, showAt = now.plusSeconds(60), now = now))
    }

    // Status and order (FR-STAR-9, FR-STAR-12)

    @Test
    fun statusFollowsTheScheduleAndSeenMark() {
        assertEquals(StarStatus.Scheduled, testStar(showAt = now.plusSeconds(1)).status(now))
        assertEquals(StarStatus.Waiting, testStar(showAt = now).status(now))
        assertEquals(StarStatus.Waiting, testStar(showAt = null).status(now))
        assertEquals(StarStatus.Seen, testStar(seenAt = now.minusSeconds(5)).status(now))
    }

    @Test
    fun onlyUnseenStarsCanBeEdited() {
        assertTrue(testStar().isEditable)
        assertFalse(testStar(seenAt = now).isEditable)
    }

    @Test
    fun waitingStarsAreShownOldestFirstByWhenTheyBecameVisible() {
        val sentForNextOpenEarly = testStar(id = "a", createdAt = now.minusSeconds(3_600))
        val scheduledInThePast = testStar(id = "b", createdAt = now.minusSeconds(86_400), showAt = now.minusSeconds(60))
        val sentForNextOpenLater = testStar(id = "c", createdAt = now.minusSeconds(30))
        val stillScheduled = testStar(id = "d", showAt = now.plusSeconds(60))
        val seen = testStar(id = "e", seenAt = now)

        val waiting = listOf(sentForNextOpenLater, stillScheduled, seen, scheduledInThePast, sentForNextOpenEarly)
            .waitingToBeShown(now)

        assertEquals(listOf("a", "b", "c"), waiting.map { it.id })
    }
}
