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

/** Our Orbit: together since, milestones and meetups (FR-ORB-4 to FR-ORB-8). */
@Serializable
data object OrbitRoute

/**
 * "When did your story begin?" (FR-ORB-2). [afterPairing] makes it skippable and continues into the
 * app, to the waiting Shooting Star when [showShootingStarNext]. Read by `TogetherSinceViewModel` (`AfterPairingKey`).
 */
@Serializable
data class TogetherSinceRoute(val afterPairing: Boolean = false, val showShootingStarNext: Boolean = false)

/**
 * Adds a meetup, edits [meetupId], or with [fromReunion] records the passed reunion (FR-ORB-3, FR-ORB-10).
 * Read by `MeetupEditorViewModel` under the same names (`MeetupIdKey`, `FromReunionKey`).
 */
@Serializable
data class MeetupEditorRoute(val meetupId: String? = null, val fromReunion: Boolean = false)

/** "About you": short name and pronouns, asked once after signing in (FR-PRO-2). */
@Serializable
data object AboutYouRoute

/** Your profile, opened from Settings (FR-PRO-4). */
@Serializable
data object ProfileRoute
