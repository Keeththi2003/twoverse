package app.twoverse.core.designsystem.text

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.twoverse.R
import app.twoverse.core.common.CompassDirection
import app.twoverse.core.common.ElapsedTime
import app.twoverse.core.common.LocationUnavailableReason
import app.twoverse.core.model.DistanceUnit

/** Display text for location values shared by Home and Your Star. */
@StringRes
fun CompassDirection.labelRes(): Int = when (this) {
    CompassDirection.North -> R.string.direction_north
    CompassDirection.NorthEast -> R.string.direction_north_east
    CompassDirection.East -> R.string.direction_east
    CompassDirection.SouthEast -> R.string.direction_south_east
    CompassDirection.South -> R.string.direction_south
    CompassDirection.SouthWest -> R.string.direction_south_west
    CompassDirection.West -> R.string.direction_west
    CompassDirection.NorthWest -> R.string.direction_north_west
}

/** "12 seconds", "5 minutes", … for sentences like "Updated 12 seconds ago". */
@Composable
fun ElapsedTime.longText(): String {
    val count = amount.toInt()
    return when (this) {
        is ElapsedTime.Seconds -> pluralStringResource(R.plurals.time_seconds, count, count)
        is ElapsedTime.Minutes -> pluralStringResource(R.plurals.time_minutes, count, count)
        is ElapsedTime.Hours -> pluralStringResource(R.plurals.time_hours, count, count)
        is ElapsedTime.Days -> pluralStringResource(R.plurals.time_days, count, count)
    }
}

/** "12s ago", "5 min ago", … for compact chips. */
@Composable
fun ElapsedTime.shortText(): String {
    val count = amount.toInt()
    return when (this) {
        is ElapsedTime.Seconds -> stringResource(R.string.time_short_seconds, count)
        is ElapsedTime.Minutes -> stringResource(R.string.time_short_minutes, count)
        is ElapsedTime.Hours -> stringResource(R.string.time_short_hours, count)
        is ElapsedTime.Days -> stringResource(R.string.time_short_days, count)
    }
}

@StringRes
fun LocationUnavailableReason.messageRes(): Int = when (this) {
    LocationUnavailableReason.SharingOff -> R.string.location_sharing_off
    LocationUnavailableReason.PartnerUnavailable -> R.string.location_partner_unavailable
}

@StringRes
fun DistanceUnit.shortLabelRes(): Int = when (this) {
    DistanceUnit.Kilometres -> R.string.unit_km
    DistanceUnit.Miles -> R.string.unit_miles
}
