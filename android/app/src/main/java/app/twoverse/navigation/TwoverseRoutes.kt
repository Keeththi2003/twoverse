package app.twoverse.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

@Serializable
data object SignInRoute

@Serializable
data object SignUpRoute

@Serializable
data object ResetPasswordRoute

@Serializable
data object PairRoute

/**
 * Shows every waiting Shooting Star, oldest first (FR-STAR-12), or with [starId] one received star
 * again (FR-STAR-13). [starId] is read by `ShootingStarViewModel` under the same name (`StarIdKey`).
 */
@Serializable
data class ShootingStarRoute(val starId: String? = null)

/** Sends a new Shooting Star, or with [starId] edits an unseen one (`StarIdKey`). */
@Serializable
data class StarComposerRoute(val starId: String? = null)

/** Sent and received Shooting Stars (FR-STAR-9, FR-STAR-13). */
@Serializable
data object ShootingStarsRoute

@Serializable
data object ReconnectRoute

@Serializable
data object HomeRoute

@Serializable
data object CompassRoute

@Serializable
data object VaultRoute

@Serializable
data object SettingsRoute

@Serializable
data object LocationSetupRoute

@Serializable
data object CountdownRoute

@Serializable
data object EditReunionRoute

/** [memoryId] is read by `MemoryViewModel` under the same name (`MemoryIdKey`). */
@Serializable
data class MemoryRoute(val memoryId: String)

@Serializable
data object AddMemoryRoute
