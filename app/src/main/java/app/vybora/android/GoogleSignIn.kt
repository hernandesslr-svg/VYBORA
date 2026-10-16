package app.vybora.android

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Native Google account selection through Credential Manager.
 * The web OAuth client ID is the audience expected by /api/auth/google.
 */
object GoogleSignIn {
    private const val WEB_CLIENT_ID =
        "491553779497-llnliort4rsqu1ijna960e417tpt82rv.apps.googleusercontent.com"

    suspend fun idToken(activity: Activity): String {
        val manager = CredentialManager.create(activity)
        val regularRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        val credential = try {
            manager.getCredential(activity, regularRequest).credential
        } catch (_: NoCredentialException) {
            // Explicit interactive Google button fallback when no saved credential is available.
            val interactiveRequest = GetCredentialRequest.Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build())
                .build()
            manager.getCredential(activity, interactiveRequest).credential
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
