package app.twoverse.core.data.supabase

import app.twoverse.core.model.DataError
import app.twoverse.core.model.DataResult
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Maps Supabase failures to [DataError]; the UI turns those into SRS section 7 messages. */
internal object SupabaseErrors {

    /** Supabase Auth (GoTrue) error codes. */
    fun fromAuthErrorCode(code: String?): DataError = when (code) {
        "invalid_credentials", "user_not_found" -> DataError.InvalidCredentials
        "email_not_confirmed" -> DataError.EmailNotConfirmed
        "email_exists", "user_already_exists", "identity_already_exists" -> DataError.EmailInUse
        "weak_password" -> DataError.WeakPassword
        "over_request_rate_limit", "over_email_send_rate_limit" -> DataError.RateLimited
        else -> DataError.Unknown
    }

    /** Error keys raised by the pairing functions in supabase/migrations. */
    fun fromRpcErrorKey(key: String?): DataError = when (key) {
        "invalid_code", "code_expired", "code_used", "own_code" -> DataError.InvalidCoupleCode
        "already_paired" -> DataError.AlreadyPaired
        "not_paired" -> DataError.NotPaired
        else -> DataError.Unknown
    }

    fun from(throwable: Throwable): DataError = when (throwable) {
        is AuthRestException -> fromAuthErrorCode(throwable.error)
        is PostgrestRestException -> fromRpcErrorKey(throwable.error)
        is HttpRequestException, is HttpRequestTimeoutException, is IOException -> DataError.Network
        else -> DataError.Unknown
    }
}

/** Runs a Supabase call and returns its result or a mapped error; cancellation still propagates. */
internal suspend fun <T> supabaseCall(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    DataResult.Failure(SupabaseErrors.from(e))
}
