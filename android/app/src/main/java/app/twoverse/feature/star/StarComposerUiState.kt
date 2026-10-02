package app.twoverse.feature.star

import app.twoverse.core.model.DataError
import app.twoverse.core.model.StarContent
import app.twoverse.core.model.StarDraftProblem
import app.twoverse.core.model.StarField
import app.twoverse.core.model.StarLayout
import app.twoverse.core.model.StarPhotoChange
import app.twoverse.core.model.StarPhotoFit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Writing or editing a Shooting Star for the partner (FR-STAR-1 to FR-STAR-8). */
data class StarComposerUiState(
    /** Editing an unseen star instead of sending a new one. */
    val isEditing: Boolean,
    /** The user's local date; the date picker starts here. */
    val today: LocalDate,
    val isLoading: Boolean = true,
    /** The star to edit couldn't be loaded; sending is blocked so nothing is overwritten by mistake. */
    val loadError: DataError? = null,
    val layout: StarLayout = StarLayout.PhotoMessage,
    /** The template last applied, highlighted in the picker. */
    val template: StarTemplate? = null,
    val eyebrow: String = "",
    val title: String = "",
    val message: String = "",
    val signature: String = "",
    val photoFit: StarPhotoFit = StarPhotoFit.Fill,
    val savedPhotoUrl: String? = null,
    val newPhotoUri: String? = null,
    val isPhotoRemoved: Boolean = false,
    val schedule: StarSchedule = StarSchedule.NextOpen,
    val openPicker: StarPicker? = null,
    val isPreviewOpen: Boolean = false,
    /** Theme of the preview; null follows the app. */
    val isPreviewDark: Boolean? = null,
    val problem: StarDraftProblem? = null,
    val error: DataError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    /** The photo to show: a newly picked one, or the saved one unless removed. */
    val photo: String? get() = newPhotoUri ?: savedPhotoUrl.takeUnless { isPhotoRemoved }

    /** Whether the star will have a photo after saving, for this layout. */
    val hasPhoto: Boolean get() = photo != null && layout.shows(StarField.Photo)

    /** What will be saved: trimmed, empty fields and fields the layout hides removed (FR-STAR-4). */
    val content: StarContent
        get() = StarContent(
            layout = layout,
            eyebrow = eyebrow,
            title = title,
            message = message,
            signature = signature,
            photoFit = photoFit,
        ).normalized()

    /** The live preview (FR-STAR-8). */
    val display: StarDisplay get() = StarDisplay(content = content, photo = photo.takeIf { hasPhoto })

    val photoChange: StarPhotoChange
        get() = when {
            newPhotoUri != null -> StarPhotoChange.Replace(newPhotoUri)
            isPhotoRemoved -> StarPhotoChange.Remove
            else -> StarPhotoChange.Keep
        }

    val canSend: Boolean get() = !isLoading && loadError == null && !isSaving

    fun shows(field: StarField): Boolean = layout.shows(field)
}

/** When the partner first sees the star (FR-STAR-7). */
sealed interface StarSchedule {
    /** The next time the partner opens Twoverse. */
    data object NextOpen : StarSchedule

    /** A date and time in the sender's time zone. */
    data class At(val date: LocalDate, val time: LocalTime) : StarSchedule
}

/** The moment the star becomes visible, in UTC; null for the next open. */
fun StarSchedule.showAt(zone: ZoneId): Instant? = when (this) {
    StarSchedule.NextOpen -> null
    is StarSchedule.At -> date.atTime(time).atZone(zone).toInstant()
}

enum class StarPicker { Date, Time }

/** The time offered first after choosing a date. */
internal val DefaultStarTime: LocalTime = LocalTime.of(9, 0)
