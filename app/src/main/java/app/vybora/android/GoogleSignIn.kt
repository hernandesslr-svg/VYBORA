package app.vybora.android

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Interactive Google sign-in with a fallback for devices where the dedicated
 * Sign in with Google option fails (including Credential Manager error 16).
 */
object GoogleSignIn {
    private const val WEB_CLIENT_ID =
        "491553779497-llnliort4rsqu1ijna960e417tpt82rv.apps.googleusercontent.com"

    suspend fun idToken(activity: Activity): String {
        val manager = CredentialManager.create(activity)

        val explicitRequest = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build())
            .build()

        val credential = try {
            manager.getCredential(activity, explicitRequest).credential
        } catch (_: GetCredentialException) {
            // Fallback: show the regular Google account picker instead of retrying
            // the same reauthentication flow that may fail on some devices.
            val fallbackOption = GetGoogleIdOption.Builder()
                .setServerClientId(WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()
            manager.getCredential(activity, fallbackRequest).credential
        }

        return parseIdToken(credential)
    }

    private fun parseIdToken(credential: Credential): String {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("O Google não retornou uma credencial compatível. Tente novamente.")
    }
}
