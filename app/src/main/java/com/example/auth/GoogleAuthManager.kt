package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CustomCredential
import android.util.Log

sealed class GoogleAuthResult {
    data class Success(val user: GoogleUser) : GoogleAuthResult()
    object NoAccountOnDevice : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
    data class Error(val message: String?, val isConfigurationError: Boolean = false) : GoogleAuthResult()
}

fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext != null) {
        if (currentContext is Activity) {
            return currentContext
        }
        if (currentContext is ContextWrapper) {
            val base = currentContext.baseContext
            if (base === currentContext || base == null) break
            currentContext = base
        } else {
            break
        }
    }
    return GoogleAuthManager.currentActivity?.get()
}

object GoogleAuthManager {
    var currentActivity: java.lang.ref.WeakReference<Activity>? = null

    // In Google Identity Services (CredentialManager), serverClientId MUST be the Web Application Client ID
    // belonging to the project (audience). Project 65327632118 (maxxbilisim-webplayer-erp) Web Client:
    private const val WEB_AUDIENCE_CLIENT_ID = "65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com"

    suspend fun signInWithGoogle(context: Context, serverClientId: String = ""): GoogleAuthResult {
        val activity = context.findActivity() ?: (context as? Activity) ?: currentActivity?.get()
        if (activity == null) {
            Log.w("GoogleAuth", "No Activity context available for CredentialManager; proceeding in test/emulator mode")
            return GoogleAuthResult.NoAccountOnDevice
        }

        val credentialManager = CredentialManager.create(activity)
        
        // If an Android installed client ID (e.g. ...76n7d7o8brpa0f3do0s3jieivh7oqm6s) is passed,
        // Google Play Services dismisses without generating token because audience must be Web Client.
        val effectiveClientId = if (serverClientId.isNotBlank() && !serverClientId.contains("76n7d7o8brpa0f3do0s3jieivh7oqm6s")) {
            serverClientId.trim()
        } else {
            WEB_AUDIENCE_CLIENT_ID
        }

        Log.i("GoogleAuth", "Initiating CredentialManager sign-in with audience: $effectiveClientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(effectiveClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
            
        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            
            var email: String? = null
            var displayName: String? = null
            var photoUrl: String? = null
            var givenName: String? = null
            var familyName: String? = null

            if (credential is GoogleIdTokenCredential) {
                email = credential.id
                displayName = credential.displayName
                photoUrl = credential.profilePictureUri?.toString()
                givenName = credential.givenName
                familyName = credential.familyName
            } else if (credential is CustomCredential) {
                try {
                    val gCred = GoogleIdTokenCredential.createFrom(credential.data)
                    email = gCred.id
                    displayName = gCred.displayName
                    photoUrl = gCred.profilePictureUri?.toString()
                    givenName = gCred.givenName
                    familyName = gCred.familyName
                } catch (e: Exception) {
                    Log.w("GoogleAuth", "createFrom failed, parsing bundle directly: ${e.message}")
                }
                if (email.isNullOrBlank()) {
                    val bundle = credential.data
                    email = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID")
                        ?: bundle.getString("id")
                        ?: bundle.getString("email")
                    displayName = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME")
                        ?: bundle.getString("displayName")
                        ?: bundle.getString("name")
                    photoUrl = bundle.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI")
                        ?: bundle.getString("profilePictureUri")
                }
            }

            if (!email.isNullOrBlank()) {
                val finalName = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
                GoogleAuthResult.Success(
                    GoogleUser(
                        id = email,
                        displayName = finalName,
                        email = email,
                        givenName = givenName,
                        familyName = familyName,
                        photoUrl = photoUrl
                    )
                )
            } else {
                GoogleAuthResult.Error("Kimlik yanıtı işlenemedi (${credential.type})")
            }
        } catch (e: NoCredentialException) {
            Log.w("GoogleAuth", "No credential available on device: ${e.message}")
            GoogleAuthResult.NoAccountOnDevice
        } catch (e: GetCredentialCancellationException) {
            Log.i("GoogleAuth", "User cancelled credential picker: ${e.message}")
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuth", "Sign-in warning with GetCredentialException: ${e.message}")
            val msg = e.message ?: ""
            val isConfigErr = msg.contains("28433") || 
                              msg.contains("Developer error", ignoreCase = true) || 
                              msg.contains("10") ||
                              msg.contains("serverClientId", ignoreCase = true)
            if (msg.contains("No credential", ignoreCase = true) || 
                msg.contains("16", ignoreCase = true) || 
                msg.contains("Activity", ignoreCase = true) ||
                msg.contains("provider", ignoreCase = true)) {
                GoogleAuthResult.NoAccountOnDevice
            } else {
                GoogleAuthResult.Error(msg, isConfigurationError = isConfigErr)
            }
        } catch (e: Exception) {
            Log.w("GoogleAuth", "Sign-in warning: ${e.message}")
            GoogleAuthResult.Error("Oturum açılamadı: ${e.localizedMessage}")
        }
    }
}

data class GoogleUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val givenName: String? = null,
    val familyName: String? = null,
    val photoUrl: String? = null
)
