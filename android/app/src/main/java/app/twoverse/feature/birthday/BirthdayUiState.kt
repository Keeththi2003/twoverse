package app.twoverse.feature.birthday

sealed interface BirthdayUiState {
    data object Loading : BirthdayUiState

    /** No birthday welcome is configured; the screen continues straight to Home. */
    data object None : BirthdayUiState

    data class Welcome(
        val message: String,
        val fromName: String,
        val isEntered: Boolean = false,
    ) : BirthdayUiState
}
