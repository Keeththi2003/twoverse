package app.twoverse.widget

import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.Anniversary
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
    reunion = WidgetReunion(daysUntil = 16, date = LocalDate.of(2026, 10, 18), isToday = false, progress = 0.6f),
    newMemoryCount = 2,
    hasWaitingStar = false,
    isOffline = false,
    orbit = WidgetOrbit(
        totalDays = 845,
        period = Period.of(2, 3, 5),
        timesMet = 7,
        anniversary = Anniversary(years = 3, date = LocalDate.of(2026, 10, 30), daysUntil = 12),
    ),
)
