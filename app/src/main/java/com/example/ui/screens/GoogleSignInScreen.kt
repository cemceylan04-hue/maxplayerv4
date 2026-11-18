package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.auth.GoogleAuthManager
import com.example.auth.GoogleAuthResult
import com.example.auth.findActivity
import com.example.model.DeviceManager
import com.example.model.OdooIntegrationManager
import com.example.model.db.AppDatabase
import com.example.model.db.UserEntity
import com.example.ui.theme.RedPrimary
import kotlinx.coroutines.launch

// Web Client ID used across Odoo, Tizen, and Android Google Identity Services
private const val DEFAULT_FALLBACK_CLIENT_ID = "65327632118-hpoe8apnlf4s9aqjhfeicvb1obv4slkc.apps.googleusercontent.com"

@Composable
fun GoogleSignInScreen(
    onSignInSuccess: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isNoAccountError by remember { mutableStateOf(false) }
    var configSha1Info by remember { mutableStateOf<String?>(null) }
    var shaCopied by remember { mutableStateOf(false) }
    var triedEmailOnlyFallback by remember { mutableStateOf(false) }
    var isSignInFocused by remember { mutableStateOf(false) }
    var isAddAccountFocused by remember { mutableStateOf(false) }
    var isCopyShaFocused by remember { mutableStateOf(false) }
    val signInFocusRequester = remember { FocusRequester() }

    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }

    // Initialize device identity and focus sign-in button for Android TV remote
    LaunchedEffect(Unit) {
        DeviceManager.init(context)
        try {
            signInFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val openAddGoogleAccountSettings: () -> Unit = {
        val intents = listOf(
            Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        var launched = false
        for (intent in intents) {
            try {
                context.startActivity(intent)
                launched = true
                break
            } catch (_: Exception) {}
        }
        if (!launched) {
            errorMessage = "Cihaz ayarları açılamadı. Lütfen Sistem Ayarları > Hesaplar menüsünden Google hesabınızı ekleyip tekrar deneyin."
        }
    }

    val resolvedClientId = remember {
        val configuredId = BuildConfig.GOOGLE_CLIENT_ID
        if (configuredId.isBlank() || configuredId == "YOUR_GOOGLE_CLIENT_ID" || configuredId == "mock-client-id") {
            DEFAULT_FALLBACK_CLIENT_ID
        } else {
            configuredId
        }
    }

    val completeSignInSuccess: (com.example.auth.GoogleUser) -> Unit = { user ->
        scope.launch {
            try {
                statusText = "Hesabınıza giriş yapılıyor..."
                val email = user.email ?: user.id
                val displayName = user.displayName ?: "Google Kullanıcısı"

                // 1. Doğrulanan Google kullanıcısını yerel veritabanına ve DeviceManager'a kaydet
                val db = AppDatabase.getDatabase(context)
                db.iptvDao().insertUser(
                    UserEntity(
                        id = email,
                        name = displayName,
                        email = email,
                        photoUrl = user.photoUrl
                    )
                )
                DeviceManager.setCustomerName(displayName)
                DeviceManager.setCurrentUser(email, displayName, email, user.photoUrl)

                // 2. Odoo müşteri ve varsa çalma listelerini hızlıca eşitle (zaman aşımı korumalı)
                kotlinx.coroutines.withTimeoutOrNull(4500L) {
                    try {
                        OdooIntegrationManager.registerCustomerAndTrial(
                            context = context,
                            userId = email,
                            userName = displayName,
                            userEmail = email
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("GoogleSignIn", "Odoo kayit uyarisi: ${e.message}")
                    }
                    try {
                        OdooIntegrationManager.syncPlaylistsFromOdoo(context, email)
                    } catch (_: Exception) {}
                }

                // 3. Doğrudan hesaba giriş yap (bilgi ekranları atlanır)
                isLoading = false
                statusText = null
                onSignInSuccess(email)
            } catch (e: Exception) {
                isLoading = false
                statusText = null
                errorMessage = "Giriş tamamlanırken hata: ${e.localizedMessage}"
            }
        }
    }

    lateinit var launchGoogleIntent: (Boolean) -> Unit
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val parsed = GoogleAuthManager.parseGoogleSignInIntent(activityResult.data)
        when (parsed) {
            is GoogleAuthResult.Success -> {
                completeSignInSuccess(parsed.user)
            }
            is GoogleAuthResult.Cancelled -> {
                isLoading = false
                statusText = null
                isNoAccountError = false
                configSha1Info = null
                errorMessage = "Google ile oturum açma iptal edildi. Lütfen hesabınızı seçip tekrar deneyiniz."
            }
            is GoogleAuthResult.NoAccountOnDevice -> {
                isLoading = false
                statusText = null
                isNoAccountError = true
                configSha1Info = null
                errorMessage = "Cihazınızda oturum açmış bir Google hesabı bulunamadı."
            }
            is GoogleAuthResult.Error -> {
                if (parsed.isConfigurationError && !triedEmailOnlyFallback) {
                    triedEmailOnlyFallback = true
                    launchGoogleIntent(false)
                } else {
                    isLoading = false
                    statusText = null
                    isNoAccountError = false
                    configSha1Info = if (parsed.isConfigurationError) {
                        GoogleAuthManager.getAppSigningSha1(context)
                    } else null
                    errorMessage = parsed.message ?: "Google ile giriş yapılırken bir hata oluştu."
                }
            }
        }
    }

    launchGoogleIntent = { includeIdToken ->
        try {
            val hostContext = context.findActivity() ?: context
            val client = GoogleAuthManager.buildGoogleSignInClient(
                context = hostContext,
                serverClientId = resolvedClientId,
                includeIdToken = includeIdToken
            )
            googleSignInLauncher.launch(client.signInIntent)
        } catch (e: Exception) {
            // Fallback to CredentialManager if GoogleSignIn intent fails to launch
            scope.launch {
                val res = GoogleAuthManager.signInWithGoogle(context, resolvedClientId)
                when (res) {
                    is GoogleAuthResult.Success -> completeSignInSuccess(res.user)
                    is GoogleAuthResult.NoAccountOnDevice -> {
                        isLoading = false
                        statusText = null
                        isNoAccountError = true
                        errorMessage = "Cihazınızda oturum açmış bir Google hesabı bulunamadı."
                    }
                    is GoogleAuthResult.Cancelled -> {
                        isLoading = false
                        statusText = null
                        errorMessage = "Google ile oturum açma iptal edildi."
                    }
                    is GoogleAuthResult.Error -> {
                        isLoading = false
                        statusText = null
                        errorMessage = res.message ?: "Google ile giriş yapılırken bir hata oluştu."
                    }
                }
            }
        }
    }

    val startGoogleSignIn: () -> Unit = {
        isLoading = true
        statusText = "Google hesabı seçiliyor..."
        errorMessage = null
        isNoAccountError = false
        configSha1Info = null
        shaCopied = false
        triedEmailOnlyFallback = false
        launchGoogleIntent(true)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0A0E17),
                        Color(0xFF06090F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // App Icon & Glow
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = stringResource(R.string.app_name),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Giriş Yap & Senkronize Et",
                color = Color(0xFFE2E8F0),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tüm oynatma listeleriniz, paketleriniz ve cihaz ayarlarınız Google hesabınızla güvenle senkronize edilir.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Error banner if any
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1318)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Giriş Uyarısı",
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        if (isNoAccountError) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = openAddGoogleAccountSettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .onFocusChanged { isAddAccountFocused = it.isFocused }
                                    .testTag("add_google_account_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isAddAccountFocused) 2.dp else 1.dp,
                                    color = if (isAddAccountFocused) Color.White else Color(0xFFEF5350)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFFCDD2)
                                )
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cihaz Ayarlarından Google Hesabı Ekle",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (configSha1Info != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A0D10), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Bu APK'nın SHA-1 imzası Google Cloud'da eksik olabilir:",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = configSha1Info!!,
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("APK SHA-1", configSha1Info))
                                        shaCopied = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .onFocusChanged { isCopyShaFocused = it.isFocused }
                                        .testTag("copy_sha1_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isCopyShaFocused) 2.dp else 1.dp,
                                        color = if (isCopyShaFocused) Color.White else Color(0xFFF59E0B)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFDE68A)
                                    )
                                ) {
                                    Icon(
                                        if (shaCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (shaCopied) "SHA-1 Kopyalandı" else "APK SHA-1 İmzasını Kopyala",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Exclusive Google Sign In Button
            Button(
                onClick = { if (!isLoading) startGoogleSignIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .focusRequester(signInFocusRequester)
                    .onFocusChanged { isSignInFocused = it.isFocused }
                    .testTag("google_signin_button"),
                shape = RoundedCornerShape(14.dp),
                border = if (isSignInFocused) BorderStroke(3.dp, RedPrimary) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 6.dp
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = RedPrimary,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = statusText ?: "Giriş yapılıyor...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F1F1F)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Google ile Giriş Yap",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Information / Features Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141E2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureItem(
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF4CAF50),
                        title = "Güvenli Oturum",
                        desc = "Şifre girmeden Google Identity güvencesiyle tek tıkla oturum açın."
                    )
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    FeatureItem(
                        icon = Icons.Default.Sync,
                        iconTint = Color(0xFF2196F3),
                        title = "Otomatik Senkronizasyon",
                        desc = "Çalma listeleriniz ve paketleriniz anında cihazınıza tanımlanır."
                    )
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    FeatureItem(
                        icon = Icons.Default.Devices,
                        iconTint = Color(0xFFFF9800),
                        title = "Çoklu Cihaz Desteği",
                        desc = "Web portalı üzerinden listelerinizi uzaktan yönetin."
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )
        }
    }
}
