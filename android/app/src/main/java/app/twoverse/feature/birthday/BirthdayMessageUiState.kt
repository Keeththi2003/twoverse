package app.twoverse.feature.birthday

import app.twoverse.core.model.BirthdayMessageDraft
import app.twoverse.core.model.BirthdayPhotoChange
import app.twoverse.core.model.DataError
import java.time.LocalDate

/** Writing the birthday welcome for the partner (FR-BDY-1). */
data class BirthdayMessageUiState(
    val today: LocalDate,
    val isLoading: Boolean = true,
    /** The saved welcome couldn't be loaded; saving is blocked so it isn't overwritten by mistake. */
    val loadError: DataError? = null,
    val message: String = "",
    /** Shown from this date; null shows it the next time the partner opens the app (FR-BDY-3). */
    val showOn: LocalDate? = null,
    val savedPhotoUrl: String? = null,
    val newPhotoUri: String? = null,
    val isPhotoRemoved: Boolean = false,
    /** The partner already saw the saved welcome; saving shows it once more. */
    val wasSeen: Boolean = false,
    val isDatePickerOpen: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: DataError? = null,
) {
    /** The photo to preview: a newly picked one, or the saved one unless removed. */
    val photo: String? get() = newPhotoUri ?: savedPhotoUrl.takeUnless { isPhotoRemoved }

    val canSave: Boolean get() = !isLoading && loadError == null && message.isNotBlank() && !isSaving

    fun toDraft() = BirthdayMessageDraft(
        message = message.trim(),
        showOn = showOn,
        photo = when {
            newPhotoUri != null -> BirthdayPhotoChange.Replace(newPhotoUri)
            isPhotoRemoved -> BirthdayPhotoChange.Remove
            else -> BirthdayPhotoChange.Keep
        },
    )
}
