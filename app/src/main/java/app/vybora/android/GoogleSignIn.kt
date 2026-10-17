package app.vybora.android

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Explicit Google sign-in flow for the visible "Continuar com Google" button.
 * Avoids the silent/authorized-account reauthentication path that can fail with
 * Credential Manager error 16 ("Account reauth failed").
 */
object GoogleSignIn {
    private const val WEB_CLIENT_ID =
        "491553779497-llnliort4rsqu1ijna960e417tpt82rv.apps.googleusercontent.com"

    suspend fun idToken(activity: Activity): String {
        val manager = CredentialManager.create(activity)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build()
            )
            .build()

        val credential = manager.getCredential(activity, request).credential
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
