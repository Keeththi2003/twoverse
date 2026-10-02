package app.twoverse.feature.star

sealed interface ShootingStarUiState {
    data object Loading : ShootingStarUiState

    /** Nothing (more) to show; the app continues. */
    data object Done : ShootingStarUiState

    data class Showing(
        val starId: String,
        val display: StarDisplay,
        /** Another waiting star follows this one (FR-STAR-12). */
        val hasNext: Boolean = false,
    ) : ShootingStarUiState
}
