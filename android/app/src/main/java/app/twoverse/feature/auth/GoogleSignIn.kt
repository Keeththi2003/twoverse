package app.twoverse.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

/** Result of the Credential Manager "Sign in with Google" sheet. */
sealed interface GoogleSignInResult {
    /** [rawNonce] goes to Supabase; Google only saw its SHA-256 hash. */
    data class Token(val idToken: String, val rawNonce: String) : GoogleSignInResult

    data object Cancelled : GoogleSignInResult

    data object Failed : GoogleSignInResult
}

/**
 * Shows the Google account sheet and returns an ID token for Supabase Auth (FR-AUTH-1).
 * [activityContext] must be an Activity so the sheet can be shown.
 */
suspend fun requestGoogleIdToken(activityContext: Context, webClientId: String): GoogleSignInResult {
    val rawNonce = UUID.randomUUID().toString()
    val option = GetSignInWithGoogleOption.Builder(webClientId)
        .setNonce(sha256(rawNonce))
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
        } else {
            GoogleSignInResult.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        // No Google account on the device.
        GoogleSignInResult.Failed
    } catch (e: GetCredentialException) {
        GoogleSignInResult.Failed
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInResult.Failed
    }
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
