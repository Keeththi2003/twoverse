package app.twoverse.core.designsystem.text

import androidx.annotation.StringRes
import app.twoverse.R
import app.twoverse.core.model.DataError

/** The message shown for a failed action (SRS section 7). Raw backend text is never shown. */
@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.Network -> R.string.offline_banner
    DataError.InvalidCoupleCode -> R.string.pair_invalid_code
    DataError.AlreadyPaired -> R.string.error_already_paired
    DataError.NotPaired -> R.string.error_not_paired
    DataError.MemoryUnavailable -> R.string.memory_expired
    DataError.StarUnavailable -> R.string.error_star_unavailable
    DataError.DateInFuture -> R.string.error_date_in_future
    DataError.ReconnectUnavailable -> R.string.error_reconnect_unavailable
    DataError.InvalidCredentials -> R.string.error_invalid_credentials
    DataError.EmailNotConfirmed -> R.string.error_email_not_confirmed
    DataError.EmailInUse -> R.string.error_email_in_use
    DataError.WeakPassword -> R.string.error_weak_password
    DataError.RateLimited -> R.string.error_rate_limited
    DataError.Unknown -> R.string.error_unknown
}
