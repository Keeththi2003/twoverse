package app.twoverse.feature.profile

import app.twoverse.core.model.DataError
import app.twoverse.core.model.Pronouns

/** Your profile (FR-PRO-4). */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val fullName: String = "",
    val shortName: String = "",
    val pronouns: Pronouns? = null,
    /** As typed; saved in international format. */
    val phone: String = "",
    val shareEmail: Boolean = false,
    val sharePhone: Boolean = false,
    val email: String? = null,
    /** A new email waiting for confirmation; [email] stays in use until then. */
    val pendingEmail: String? = null,
    /** False for Google-only accounts: their email is shown read-only. */
    val canChangeEmail: Boolean = false,
    val isEmailDialogOpen: Boolean = false,
    val newEmail: String = "",
    val isNewEmailInvalid: Boolean = false,
    /** The confirmation email was sent: "Check your new email to confirm". */
    val isEmailChangeSent: Boolean = false,
    val problem: ProfileProblem? = null,
    val error: DataError? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !isSaving
}

enum class ProfileProblem { FullNameMissing, ShortNameMissing, PhoneInvalid }
