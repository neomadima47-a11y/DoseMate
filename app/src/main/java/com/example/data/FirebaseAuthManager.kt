package com.example.data

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseAuthManager {

    private const val TAG = "FirebaseAuthManager"

    // Default Web Client ID for dosemate-b6391
    const val DEFAULT_WEB_CLIENT_ID = "438560250460-dosemate.apps.googleusercontent.com"

    fun sendFirebasePasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val auth = FirebaseAuth.getInstance()
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onError(task.exception?.localizedMessage ?: "Failed to send reset email.")
                    }
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Firebase Auth error")
        }
    }

    fun signInWithGoogleFirebase(
        activity: Activity,
        webClientId: String = DEFAULT_WEB_CLIENT_ID,
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val firebaseUser = task.result?.user
                                val name = firebaseUser?.displayName ?: "Google User"
                                val email = firebaseUser?.email ?: ""
                                onComplete(true, email, name)
                            } else {
                                Log.e(TAG, "Firebase Google Auth task error", task.exception)
                                onComplete(false, null, null)
                            }
                        }
                } else {
                    onComplete(false, null, null)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Google Credential Manager canceled or failed: ${e.message}")
                onComplete(false, null, null)
            }
        }
    }
}
