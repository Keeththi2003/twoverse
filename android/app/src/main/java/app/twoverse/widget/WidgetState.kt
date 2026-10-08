package app.twoverse.widget

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.Anniversary
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.common.PartnerName
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.common.ReunionPhase
import app.twoverse.core.common.countdownProgress
import app.twoverse.core.common.countdownUntil
import app.twoverse.core.common.formatDistance
import app.twoverse.core.common.nextAnniversary
import app.twoverse.core.common.partnerPosition
import app.twoverse.core.common.reunionPhase
import app.twoverse.core.common.toPartnerName
import app.twoverse.core.common.togetherDuration
import app.twoverse.core.data.local.OfflineSnapshot
import app.twoverse.core.model.DistanceUnit
import app.twoverse.core.model.isNew
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Locale

/** Why the widget can or can't show the distance (FR-WGT-6). */
enum class WidgetLocation {
    /** Signed out or not connected to a partner yet. */
    NotPaired,

    /** This user's own location sharing is off. */
    SharingOff,

    /** One of the two locations isn't known. */
    Unavailable,

    Available,
}

/** What the widget shows (FR-WGT-1). Never photos or captions (FR-WGT-3). */
data class WidgetState(
    val location: WidgetLocation,
    /** Formatted distance number, or null unless [location] is available. */
    val distance: String?,
    val distanceUnit: DistanceUnit,
    val freshness: LocationFreshness,
    /** How long ago her position was recorded, for "Updated 5 min ago" and "Last seen …". */
    val updatedAgo: ElapsedTime?,
    /** Her direction from this user. */
    val direction: CompassDirection?,
    /** Her time zone, only when it differs from this phone's right now. */
    val partnerTimeZone: ZoneId?,
    /** The upcoming reunion, or null when no date is set or it has passed. */
    val reunion: WidgetReunion?,
    val newMemoryCount: Int,
    /** A Shooting Star is waiting to be opened (only the fact, never its content). */
    val hasWaitingStar: Boolean,
    /** No connection: the widget says it shows saved data. */
    val isOffline: Boolean,
    /** The partner's name and pronouns, as the app shows them (FR-PRO-1); null when unpaired. */
    val partner: PartnerName? = null,
    /** How long they've been together; null when "together since" isn't set, so that part is left out (FR-WGT-8). */
    val orbit: WidgetOrbit? = null,
) {
    val isPaired: Boolean get() = location != WidgetLocation.NotPaired
}

data class WidgetReunion(
    val daysUntil: Long,
    /** The reunion day in this user's time zone. */
    val date: LocalDate,
    val isToday: Boolean,
    /** How much of the wait has passed since the date was set, 0..1 ("getting closer"). */
    val progress: Float,
)

data class WidgetOrbit(
    /** The day number; the start date is day 1 (FR-ORB). */
    val totalDays: Long,
    val period: Period,
    val timesMet: Int,
    /** The next anniversary, only when it is within [AnniversarySoonDays]. */
    val anniversary: Anniversary?,
)

/** The large widget mentions the next anniversary from this many days before it. */
internal const val AnniversarySoonDays = 30L

/**
 * The widget reads only the data saved on the device, so it works without a connection.
 * Freshness comes from when the partner's position was recorded, so saved data turns
 * "Recent" and then "Outdated" and is never shown as live (BR-8).
 */
internal fun widgetState(
    snapshot: OfflineSnapshot?,
    distanceUnit: DistanceUnit,
    isOnline: Boolean,
    now: Instant,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): WidgetState {
    val couple = snapshot?.couple
    val position = partnerPosition(
        myLocation = snapshot?.myLocation,
        partnerLocation = snapshot?.partnerLocation?.takeIf { couple != null },
        sharingEnabled = snapshot?.sharing?.enabled == true,
        now = now,
    )
    val location = when {
        couple == null -> WidgetLocation.NotPaired
        position.unavailableReason == LocationUnavailableReason.SharingOff -> WidgetLocation.SharingOff
        position.distanceKm == null -> WidgetLocation.Unavailable
        else -> WidgetLocation.Available
    }
    val available = location == WidgetLocation.Available
    return WidgetState(
        location = location,
        distance = position.distanceKm?.takeIf { available }?.let { formatDistance(it, distanceUnit, locale) },
        distanceUnit = distanceUnit,
        freshness = if (available) position.freshness else LocationFreshness.Unavailable,
        updatedAgo = position.updatedAgo?.takeIf { available },
        direction = position.bearingDegrees?.takeIf { available }?.let(CompassDirection::fromBearing),
        partnerTimeZone = couple?.partner?.timeZone?.let { differentZone(it, zone, now) },
        reunion = couple?.let { snapshot.reunion }?.let { reunion ->
            when (reunionPhase(reunion.meetAt, now, zone)) {
                ReunionPhase.Past -> null
                ReunionPhase.Today -> WidgetReunion(0, reunion.meetAt.atZone(zone).toLocalDate(), isToday = true, progress = 1f)
                ReunionPhase.Upcoming -> WidgetReunion(
                    daysUntil = countdownUntil(reunion.meetAt, now).days,
                    date = reunion.meetAt.atZone(zone).toLocalDate(),
                    isToday = false,
                    progress = countdownProgress(reunion.dateSetAt, reunion.meetAt, now),
                )
            }
        },
        newMemoryCount = couple?.let { snapshot.memories?.count { it.isNew } } ?: 0,
        hasWaitingStar = couple != null && snapshot.hasWaitingStar == true,
        isOffline = !isOnline,
        partner = couple?.partner?.toPartnerName(),
        orbit = couple?.togetherSince?.let { since ->
            val today = now.atZone(zone).toLocalDate()
            togetherDuration(since, today)?.let { duration ->
                WidgetOrbit(
                    totalDays = duration.totalDays,
                    period = duration.period,
                    timesMet = snapshot.meetups?.size ?: 0,
                    anniversary = nextAnniversary(since, today).takeIf { it.daysUntil <= AnniversarySoonDays },
                )
            }
        },
    )
}

/** Her zone when its clock differs from this phone's at [now]; null when the same or not a real zone. */
private fun differentZone(id: String, mine: ZoneId, now: Instant): ZoneId? {
    val hers = try {
        ZoneId.of(id)
    } catch (e: DateTimeException) {
        return null
    }
    return hers.takeIf { it.rules.getOffset(now) != mine.rules.getOffset(now) }
}
