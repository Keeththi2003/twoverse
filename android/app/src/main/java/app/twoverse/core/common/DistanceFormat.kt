package app.twoverse.core.common

import app.twoverse.core.model.DistanceUnit
import java.util.Locale
import kotlin.math.roundToLong

private const val KmPerMile = 1.609344
private const val OneDecimalBelow = 100.0

/**
 * Distance number for display (FR-LOC-9): one decimal under 100, whole numbers from 100 up with
 * the locale's thousands separator ("14,285"), in the user's unit. The unit label is added by the UI.
 */
fun formatDistance(km: Double, unit: DistanceUnit, locale: Locale = Locale.getDefault()): String {
    val value = when (unit) {
        DistanceUnit.Kilometres -> km
        DistanceUnit.Miles -> km / KmPerMile
    }
    val rounded = Math.round(value * 10) / 10.0
    return if (rounded < OneDecimalBelow) {
        String.format(locale, "%.1f", rounded)
    } else {
        String.format(locale, "%,d", value.roundToLong())
    }
}
