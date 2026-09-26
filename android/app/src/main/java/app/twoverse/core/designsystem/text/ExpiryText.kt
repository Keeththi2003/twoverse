package app.twoverse.core.designsystem.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import app.twoverse.R
import app.twoverse.core.common.ExpiryBadge

/** "24 hours", "2 days", … for "Expires in 2 days" (Vault, Memory viewer). */
@Composable
fun ExpiryBadge.longText(): String {
    val count = amount.toInt()
    return when (this) {
        is ExpiryBadge.Hours -> pluralStringResource(R.plurals.time_hours, count, count)
        is ExpiryBadge.Days -> pluralStringResource(R.plurals.time_days, count, count)
    }
}
