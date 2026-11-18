package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.MutableContextWrapper
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume

sealed class GoogleAuthResult {
    data class Success(val user: GoogleUser) : GoogleAuthResult()
    object NoAccountOnDevice : GoogleAuthResult()
    data class Cancelled(val detail: String? = null) : GoogleAuthResult()
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

    // In Google Identity Services, serverClientId MUST be the Web Application Client ID
    // belonging to the project (audience). Project 65327632118 (maxxbilisim-webplayer-erp) Web Client:
    private const val WEB_AUDIENCE_CLIENT_ID = "65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com"

    @Suppress("DEPRECATION")
    fun getAppSigningSha1(context: Context): String? {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                    else signingInfo.signingCertificateHistory
                } else null
            } else {
                pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNATURES).signatures
            }
            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
            val digest = java.security.MessageDigest.getInstance("SHA-1").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.w("GoogleAuth", "SHA-1 read error: ${e.message}")
            null
        }
    }

    private fun resolveWebClientId(serverClientId: String): String {
        return if (serverClientId.isNotBlank() && !serverClientId.contains("76n7d7o8brpa0f3do0s3jieivh7oqm6s")) {
            serverClientId.trim()
        } else {
            WEB_AUDIENCE_CLIENT_ID
        }
    }

    @Suppress("DEPRECATION")
    fun buildGoogleSignInClient(
        context: Context,
        serverClientId: String = "",
        includeIdToken: Boolean = true
    ): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        if (includeIdToken) {
            builder.requestIdToken(resolveWebClientId(serverClientId))
        }
        return GoogleSignIn.getClient(context, builder.build())
    }

    @Suppress("DEPRECATION")
    fun parseGoogleSignInIntent(data: Intent?): GoogleAuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: account.account?.name ?: account.id
            if (email.isNullOrBlank()) {
                return GoogleAuthResult.Error("Google hesap e-posta bilgisi alınamadı.")
            }
            val finalName = account.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            GoogleAuthResult.Success(
                GoogleUser(
                    id = email,
                    displayName = finalName,
                    email = email,
                    givenName = account.givenName,
                    familyName = account.familyName,
                    photoUrl = account.photoUrl?.toString()
                )
            )
        } catch (e: ApiException) {
            val code = e.statusCode
            Log.w("GoogleAuth", "GoogleSignIn ApiException status=$code msg=${e.message}")
            when (code) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                CommonStatusCodes.CANCELED -> GoogleAuthResult.Cancelled("Kod: $code")
                CommonStatusCodes.DEVELOPER_ERROR -> GoogleAuthResult.Error(
                    "Google OAuth SHA-1 doğrulama hatası (Kod: 10 - DEVELOPER_ERROR)",
                    isConfigurationError = true
                )
                GoogleSignInStatusCodes.SIGN_IN_FAILED -> GoogleAuthResult.Error(
                    "Google oturum açma başarısız (Kod: 12500 - SIGN_IN_FAILED)",
                    isConfigurationError = true
                )
                CommonStatusCodes.NETWORK_ERROR -> GoogleAuthResult.Error(
                    "İnternet bağlantısı sağlanamadı. Lütfen ağınızı kontrol edip tekrar deneyin."
                )
                else -> GoogleAuthResult.Error(
                    "Google ile oturum açılamadı (Kod: $code)${e.localizedMessage?.let { ": $it" } ?: ""}"
                )
            }
        } catch (e: Exception) {
            Log.w("GoogleAuth", "GoogleSignIn unexpected error: ${e.message}")
            GoogleAuthResult.Error("Google ile oturum açılamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}")
        }
    }

    private fun generateSecureRandomNonce(byteLength: Int = 32): String {
        val randomBytes = ByteArray(byteLength)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }

    @Suppress("DEPRECATION")
    suspend fun clearCredentialState(context: Context) {
        try {
            val appCtx = context.applicationContext
            val credentialManager = CredentialManager.create(appCtx)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("GoogleAuth", "clearCredentialState skipped: ${e.message}")
        }
        try {
            val client = buildGoogleSignInClient(context.applicationContext, includeIdToken = false)
            suspendCancellableCoroutine<Unit> { cont ->
                client.signOut().addOnCompleteListener {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleAuth", "GoogleSignIn signOut skipped: ${e.message}")
        }
    }

    suspend fun signInWithGoogle(context: Context, serverClientId: String = ""): GoogleAuthResult {
        val activity = context.findActivity() ?: (context as? Activity) ?: currentActivity?.get()
        if (activity == null) {
            Log.w("GoogleAuth", "No Activity context available for CredentialManager")
            return GoogleAuthResult.Error("Giriş penceresi başlatılamadı (Activity bulunamadı).")
        }

        val credentialManager = CredentialManager.create(activity)
        val mutableContext = MutableContextWrapper(activity)

        // If an Android installed client ID (e.g. ...76n7d7o8brpa0f3do0s3jieivh7oqm6s) is passed,
        // Google Play Services rejects it because serverClientId MUST be the Web Application Client ID.
        val effectiveClientId = resolveWebClientId(serverClientId)

        Log.i("GoogleAuth", "Initiating CredentialManager button sign-in (GetSignInWithGoogleOption) with audience: $effectiveClientId")

        // 1. Primary: Official Google button flow (GetSignInWithGoogleOption).
        // Works even when One-Tap prompts are disabled in Google settings, when re-auth is needed,
        // or when no Google account has authorized the app yet.
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(effectiveClientId)
            .setNonce(generateSecureRandomNonce())
            .build()

        val buttonRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        try {
            val result = credentialManager.getCredential(mutableContext, buttonRequest)
            return parseCredentialResponse(result)
        } catch (e: GetCredentialCancellationException) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e.errorMessage?.toString()
            Log.w("GoogleAuth", "GetSignInWithGoogleOption cancelled (type=${e.type}): $detail")
            return GoogleAuthResult.Cancelled(detail)
        } catch (e: NoCredentialException) {
            Log.w("GoogleAuth", "GetSignInWithGoogleOption NoCredentialException: ${e.message}; trying GetGoogleIdOption fallback")
        } catch (e: GetCredentialUnsupportedException) {
            Log.w("GoogleAuth", "GetSignInWithGoogleOption unsupported: ${e.message}; trying GetGoogleIdOption fallback")
        } catch (e: GetCredentialException) {
            val mapped = mapGetCredentialException(e)
            if (mapped !is GoogleAuthResult.NoAccountOnDevice) {
                return mapped
            }
            Log.w("GoogleAuth", "GetSignInWithGoogleOption returned ${e.message}; trying GetGoogleIdOption fallback")
        } catch (e: Exception) {
            Log.w("GoogleAuth", "Unexpected error on GetSignInWithGoogleOption: ${e.message}")
        }

        // 2. Secondary fallback: GetGoogleIdOption (broad account lookup without authorized filter)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(effectiveClientId)
            .setAutoSelectEnabled(false)
            .setNonce(generateSecureRandomNonce())
            .build()

        val bottomSheetRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(mutableContext, bottomSheetRequest)
            parseCredentialResponse(result)
        } catch (e: GetCredentialCancellationException) {
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e.errorMessage?.toString()
            Log.w("GoogleAuth", "GetGoogleIdOption cancelled (type=${e.type}): $detail")
            GoogleAuthResult.Cancelled(detail)
        } catch (e: NoCredentialException) {
            Log.w("GoogleAuth", "No Google credential available on device: ${e.message}")
            GoogleAuthResult.NoAccountOnDevice
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.w("GoogleAuth", "Credential provider missing: ${e.message}")
            GoogleAuthResult.Error("Bu cihazda Google Play Hizmetleri kimlik sağlayıcısı bulunamadı veya güncel değil.")
        } catch (e: GetCredentialUnsupportedException) {
            Log.w("GoogleAuth", "Credential Manager unsupported on device: ${e.message}")
            GoogleAuthResult.Error("Bu cihaz Google Kimlik Yöneticisi (Credential Manager) özelliğini desteklemiyor.")
        } catch (e: GetCredentialException) {
            mapGetCredentialException(e)
        } catch (e: Exception) {
            Log.w("GoogleAuth", "Sign-in warning: ${e.message}")
            GoogleAuthResult.Error("Oturum açılamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}")
        }
    }

    private fun parseCredentialResponse(result: GetCredentialResponse): GoogleAuthResult {
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

        return if (!email.isNullOrBlank()) {
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
    }

    private fun mapGetCredentialException(e: GetCredentialException): GoogleAuthResult {
        val msg = e.message?.takeIf { it.isNotBlank() } ?: e.errorMessage?.toString() ?: ""
        Log.w("GoogleAuth", "GetCredentialException (${e::class.java.simpleName}, type=${e.type}): $msg")

        // Check OAuth / SHA-1 developer configuration errors FIRST so they are never hidden as "NoAccountOnDevice"
        val isConfigErr = msg.contains("28444") ||
            msg.contains("28433") ||
            msg.contains("Developer console", ignoreCase = true) ||
            msg.contains("Developer error", ignoreCase = true) ||
            msg.contains("DEVELOPER_ERROR", ignoreCase = true) ||
            msg.contains("serverClientId", ignoreCase = true) ||
            msg.contains("10:")

        if (isConfigErr) {
            return GoogleAuthResult.Error(msg, isConfigurationError = true)
        }

        if (e is GetCredentialProviderConfigurationException) {
            return GoogleAuthResult.Error("Bu cihazda Google Play Hizmetleri kimlik sağlayıcısı bulunamadı veya güncel değil.")
        }
        if (e is GetCredentialUnsupportedException) {
            return GoogleAuthResult.Error("Bu cihaz Google Kimlik Yöneticisi (Credential Manager) özelliğini desteklemiyor.")
        }

        if (msg.contains("No credential", ignoreCase = true) ||
            msg.contains("Cannot find a matching credential", ignoreCase = true) ||
            msg.contains("No matching credentials", ignoreCase = true)) {
            return GoogleAuthResult.NoAccountOnDevice
        }

        return GoogleAuthResult.Error(msg.ifBlank { "Google ile oturum açılamadı." }, isConfigurationError = false)
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
