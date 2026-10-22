package app.vybora.android

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Legacy Play Services sign-in is used as an interactive account-picker fallback
 * because some devices return "No credentials available" from Credential Manager.
 */
object GoogleSignIn {
    private const val WEB_CLIENT_ID =
        "491553779497-bsgfj74l57u0gffi68jv6131joso0o5u.apps.googleusercontent.com"

    private fun client(activity: Activity) =
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(WEB_CLIENT_ID)
                .build()
        )

    fun signOut(activity: Activity, onComplete: () -> Unit) {
        // Clear the remembered Google account before opening the picker again.
        client(activity).signOut().addOnCompleteListener {
            onComplete()
        }
    }

    fun signInIntent(activity: Activity): Intent = client(activity).signInIntent

    fun idTokenFromResult(data: Intent?): String {
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getSignedInAccountFromIntent(data)
        val account: GoogleSignInAccount = try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            throw IllegalStateException(
                "Falha no login Google (código ${e.statusCode}). Confira se há uma conta Google adicionada ao aparelho e tente novamente.",
                e
            )
        }
        return account.idToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("O Google não retornou o token de autenticação. Verifique o cliente OAuth Web.")
    }
}
