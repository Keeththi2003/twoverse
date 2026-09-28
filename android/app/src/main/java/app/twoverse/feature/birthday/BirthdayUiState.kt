package app.twoverse.feature.birthday

sealed interface BirthdayUiState {
    data object Loading : BirthdayUiState

    /** No birthday welcome is configured; the screen continues straight on. */
    data object None : BirthdayUiState

    data class Welcome(
        val message: String,
        val fromName: String,
        /** Optional photo from the partner (FR-BDY-1). */
        val photoUrl: String? = null,
        val isEntered: Boolean = false,
    ) : BirthdayUiState
}
