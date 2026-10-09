package app.twoverse.widget

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationFreshness
import app.twoverse.core.model.DistanceUnit
import java.time.LocalDate
import java.time.Period

/** Sample data for the widget picker preview and Glance previews; never real data. */
internal val SampleWidgetState = WidgetState(
    location = WidgetLocation.Available,
    distance = "94.6",
    distanceUnit = DistanceUnit.Kilometres,
    freshness = LocationFreshness.Live,
    updatedAgo = ElapsedTime.Seconds(20),
    direction = CompassDirection.NorthEast,
    partnerTimeZone = null,
    reunion = WidgetReunion(daysUntil = 6, date = LocalDate.of(2026, 10, 16), isToday = false, progress = 0.6f),
    newMemoryCount = 2,
    hasWaitingStar = false,
    isOffline = false,
    orbit = WidgetOrbit(
        since = LocalDate.of(2025, 6, 22),
        totalDays = 475,
        period = Period.of(1, 3, 17),
        timesMet = 2,
        nextMilestone = 500,
        daysToMilestone = 25,
        milestoneProgress = 110f / 135f,
    ),
)
