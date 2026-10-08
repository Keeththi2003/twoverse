package app.twoverse.core.data.push

import app.twoverse.core.model.LaunchScreen

/**
 * A push received from send-push. Only the type is ever sent, never photos or captions
 * (FR-NOT-1), so the app shows its own text.
 */
enum class IncomingPush(val type: String, val opens: LaunchScreen?) {
    /** Silent: upload a fresh location, show nothing (FR-LOC-5, FR-NOT-5). */
    WakeUp("wake_up", null),
    PartnerJoined("partner_joined", LaunchScreen.Home),
    ReunionDay("reunion_day", LaunchScreen.Countdown),
    NewMemory("new_memory", LaunchScreen.Vault),

    /** A Shooting Star became visible; opening the app shows it (FR-STAR-17). */
    ShootingStar("shooting_star", LaunchScreen.ShootingStar),

    /** Their anniversary; the push carries the number of years (FR-NOT-6). */
    Anniversary("anniversary", LaunchScreen.Orbit),

    /** A day milestone such as day 1000; the push carries the day number (FR-NOT-6). */
    OrbitMilestone("orbit_milestone", LaunchScreen.Orbit),
    ;

    companion object {
        fun fromType(type: String?): IncomingPush? = entries.firstOrNull { it.type == type }
    }
}
