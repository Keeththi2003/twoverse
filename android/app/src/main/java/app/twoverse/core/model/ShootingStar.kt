package app.twoverse.core.model

import java.time.Instant

/** How a Shooting Star is laid out (FR-STAR-2). */
enum class StarLayout {
    /** Photo card (or the orbit when there is no photo), eyebrow, title, message and signature. */
    PhotoMessage,

    /** No photo; larger, vertically centred text. */
    MessageOnly,

    /** The photo fills the screen, with an optional short title and message on top. */
    FullPhoto,
    ;

    /** The fields this layout shows; the composer hides the others and they are not saved (FR-STAR-4). */
    val fields: Set<StarField>
        get() = when (this) {
            PhotoMessage -> setOf(StarField.Eyebrow, StarField.Title, StarField.Message, StarField.Signature, StarField.Photo)
            MessageOnly -> setOf(StarField.Eyebrow, StarField.Title, StarField.Message, StarField.Signature)
            FullPhoto -> setOf(StarField.Photo, StarField.PhotoFit, StarField.Title, StarField.Message)
        }

    fun shows(field: StarField): Boolean = field in fields
}

enum class StarField { Eyebrow, Title, Message, Signature, Photo, PhotoFit }

/** How a Full photo star fills the screen (FR-STAR-6). */
enum class StarPhotoFit {
    /** Cropped to fill the screen. */
    Fill,

    /** The whole image, letterboxed. */
    Fit,
}

/** The words and look of a Shooting Star. Empty fields are null and never shown (FR-STAR-10). */
data class StarContent(
    val layout: StarLayout,
    val eyebrow: String? = null,
    val title: String? = null,
    val message: String? = null,
    val signature: String? = null,
    val photoFit: StarPhotoFit = StarPhotoFit.Fill,
) {
    /** Trimmed, with blank fields and fields the layout doesn't show removed. */
    fun normalized(): StarContent = StarContent(
        layout = layout,
        eyebrow = eyebrow.keptFor(StarField.Eyebrow),
        title = title.keptFor(StarField.Title),
        message = message.keptFor(StarField.Message),
        signature = signature.keptFor(StarField.Signature),
        photoFit = if (layout.shows(StarField.PhotoFit)) photoFit else StarPhotoFit.Fill,
    )

    private fun String?.keptFor(field: StarField): String? = this?.trim()?.ifEmpty { null }?.takeIf { layout.shows(field) }

    companion object {
        /** Limits match the shooting_stars table (FR-STAR-5). */
        const val MaxEyebrowLength = 40
        const val MaxTitleLength = 80
        const val MaxMessageLength = 1000
        const val MaxSignatureLength = 50
    }
}

/** A Shooting Star this user sent or received. */
data class ShootingStar(
    val id: String,
    val content: StarContent,
    val hasPhoto: Boolean,
    /** Short-lived signed URL (NFR-SEC-2); only loaded when a single star is opened. */
    val photoUrl: String? = null,
    /** When it becomes visible; null means the next time the partner opens the app. */
    val showAt: Instant?,
    val seenAt: Instant?,
    val createdAt: Instant,
) {
    /** When the recipient could first see it, for showing waiting stars oldest first (FR-STAR-12). */
    val visibleFrom: Instant get() = showAt ?: createdAt

    fun status(now: Instant): StarStatus = when {
        seenAt != null -> StarStatus.Seen
        showAt != null && showAt.isAfter(now) -> StarStatus.Scheduled
        else -> StarStatus.Waiting
    }

    /** The sender can edit or delete it until the partner has seen it (FR-STAR-9). */
    val isEditable: Boolean get() = seenAt == null
}

enum class StarStatus {
    /** Its time hasn't come yet. */
    Scheduled,

    /** Visible, but not opened yet. */
    Waiting,

    /** Opened by the recipient. */
    Seen,
}

/** The stars to show on this app open, oldest first (FR-STAR-12). */
fun List<ShootingStar>.waitingToBeShown(now: Instant): List<ShootingStar> =
    filter { it.status(now) == StarStatus.Waiting }.sortedBy { it.visibleFrom }

/** What the sender saves. [showAt] null shows it the next time the partner opens the app. */
data class ShootingStarDraft(
    val content: StarContent,
    val photo: StarPhotoChange,
    val showAt: Instant?,
)

sealed interface StarPhotoChange {
    data object Keep : StarPhotoChange

    data object Remove : StarPhotoChange

    data class Replace(val photoUri: String) : StarPhotoChange
}

/** Why a draft can't be sent yet (FR-STAR-5, FR-STAR-7). */
enum class StarDraftProblem {
    /** Full photo needs a photo. */
    PhotoRequired,

    /** Nothing would be shown: no photo, title or message. */
    NothingToShow,

    /** The chosen time has already passed. */
    ShowAtInPast,
}

/**
 * Checks a star before it is sent. [content] should be [StarContent.normalized]; [hasPhoto] says
 * whether the star will have a photo after saving.
 */
fun validateStar(content: StarContent, hasPhoto: Boolean, showAt: Instant?, now: Instant): StarDraftProblem? {
    val photo = hasPhoto && content.layout.shows(StarField.Photo)
    val text = content.title != null || content.message != null
    return when {
        content.layout == StarLayout.FullPhoto && !photo -> StarDraftProblem.PhotoRequired
        !photo && !text -> StarDraftProblem.NothingToShow
        showAt != null && !showAt.isAfter(now) -> StarDraftProblem.ShowAtInPast
        else -> null
    }
}
