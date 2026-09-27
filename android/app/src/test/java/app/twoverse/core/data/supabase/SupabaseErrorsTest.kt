package app.twoverse.core.data.supabase

import app.twoverse.core.model.DataError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class SupabaseErrorsTest {

    @Test
    fun authErrorCodesMapToUserFacingErrors() {
        assertEquals(DataError.InvalidCredentials, SupabaseErrors.fromAuthErrorCode("invalid_credentials"))
        assertEquals(DataError.EmailNotConfirmed, SupabaseErrors.fromAuthErrorCode("email_not_confirmed"))
        assertEquals(DataError.EmailInUse, SupabaseErrors.fromAuthErrorCode("user_already_exists"))
        assertEquals(DataError.EmailInUse, SupabaseErrors.fromAuthErrorCode("email_exists"))
        assertEquals(DataError.WeakPassword, SupabaseErrors.fromAuthErrorCode("weak_password"))
        assertEquals(DataError.RateLimited, SupabaseErrors.fromAuthErrorCode("over_email_send_rate_limit"))
    }

    @Test
    fun everyCoupleCodeFailureIsOneInvalidCodeError() {
        listOf("invalid_code", "code_expired", "code_used", "own_code").forEach {
            assertEquals(it, DataError.InvalidCoupleCode, SupabaseErrors.fromRpcErrorKey(it))
        }
    }

    @Test
    fun pairingStateErrorsAreKept() {
        assertEquals(DataError.AlreadyPaired, SupabaseErrors.fromRpcErrorKey("already_paired"))
        assertEquals(DataError.NotPaired, SupabaseErrors.fromRpcErrorKey("not_paired"))
    }

    @Test
    fun unknownCodesAreUnknown() {
        assertEquals(DataError.Unknown, SupabaseErrors.fromAuthErrorCode("something_new"))
        assertEquals(DataError.Unknown, SupabaseErrors.fromAuthErrorCode(null))
        assertEquals(DataError.Unknown, SupabaseErrors.fromRpcErrorKey("not_authenticated"))
        assertEquals(DataError.Unknown, SupabaseErrors.fromRpcErrorKey(null))
    }

    @Test
    fun sendPushStatusesMapToErrors() {
        assertEquals(DataError.NotPaired, SupabaseErrors.fromFunctionStatus(403))
        assertEquals(DataError.RateLimited, SupabaseErrors.fromFunctionStatus(429))
        assertEquals(DataError.Unknown, SupabaseErrors.fromFunctionStatus(503))
    }

    @Test
    fun connectionProblemsAreNetworkErrors() {
        assertEquals(DataError.Network, SupabaseErrors.from(IOException("no route")))
        assertEquals(DataError.Unknown, SupabaseErrors.from(IllegalStateException("bug")))
    }
}
