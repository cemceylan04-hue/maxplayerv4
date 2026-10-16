package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.LockedChannelInfo
import com.example.model.ParentalControlManager
import com.example.model.SupportRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ParentalControlDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isImeVisible = WindowInsets.isImeVisible

    val currentPin by ParentalControlManager.pinCode.collectAsState()
    val secretQuestion by ParentalControlManager.secretQuestion.collectAsState()
    val secretAnswer by ParentalControlManager.secretAnswer.collectAsState()
    val lockedChannels by ParentalControlManager.lockedChannels.collectAsState()
    val lockedGroups by ParentalControlManager.lockedGroups.collectAsState()

    var isResetMode by remember { mutableStateOf(false) }
    var isRemoveMode by remember { mutableStateOf(false) }
    var removeStep by remember { mutableStateOf(1) }
    var inputPin by remember { mutableStateOf("") }

    var newPin by remember { mutableStateOf("") }
    var questionInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Focus requesters for strict D-pad sequence
    val pinFocusRequester = remember { FocusRequester() }
    val questionFocusRequester = remember { FocusRequester() }
    val answerFocusRequester = remember { FocusRequester() }
    val saveButtonFocusRequester = remember { FocusRequester() }
    val cancelButtonFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }
    val resetPinButtonFocusRequester = remember { FocusRequester() }

    // BringIntoView requesters for TV auto-scroll
    val pinBringRequester = remember { BringIntoViewRequester() }
    val questionBringRequester = remember { BringIntoViewRequester() }
    val answerBringRequester = remember { BringIntoViewRequester() }
    val saveBringRequester = remember { BringIntoViewRequester() }

    // Focus state trackers for TV visual highlights
    var isPinFocused by remember { mutableStateOf(false) }
    var isQuestionFocused by remember { mutableStateOf(false) }
    var isAnswerFocused by remember { mutableStateOf(false) }
    var isSaveButtonFocused by remember { mutableStateOf(false) }
    var isCancelButtonFocused by remember { mutableStateOf(false) }

    // Autofocus initial field depending on mode
    LaunchedEffect(currentPin, isResetMode, isRemoveMode, removeStep) {
        delay(120)
        try {
            when {
                currentPin == null -> pinFocusRequester.requestFocus()
                isResetMode -> answerFocusRequester.requestFocus()
                isRemoveMode && removeStep == 1 -> pinFocusRequester.requestFocus()
                isRemoveMode && removeStep == 2 -> answerFocusRequester.requestFocus()
                else -> firstItemFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .imePadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isImeVisible) 6.dp else 12.dp
                ),
            contentAlignment = if (isImeVisible) Alignment.TopCenter else Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 360.dp, max = 560.dp)
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .heightIn(max = if (isImeVisible) 280.dp else 520.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E2C),
                border = BorderStroke(1.5.dp, Color(0xFF3B3B54)),
                tonalElevation = 10.dp
            ) {
                com.example.util.KeepSystemBarsHidden()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Header (Icon + Title + Subtitle)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE53935).copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    currentPin == null -> "Ebeveyn Kontrolü (PIN Kurulumu)"
                                    isResetMode -> stringResource(R.string.parental_reset_title)
                                    isRemoveMode -> "PIN Kaldırma"
                                    else -> "Ebeveyn Kontrolü (Kilitli İçerikler & PIN)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    currentPin == null -> "TV kumandası ile PIN ve gizli sorunuzu belirleyin"
                                    isResetMode -> "Gizli sorunuzu yanıtlayarak yeni PIN oluşturun"
                                    isRemoveMode -> "PIN korumasını tamamen devre dışı bırakın"
                                    else -> "Kilitli kanalları yönetin veya PIN ayarlarını değiştirin"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFB0B0C0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp).focusable(false)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentPin == null) {
                        // ==========================================
                        // MOD 1: PIN ve Gizli Soru Kurulumu
                        // Sıralama:
                        // 1. PIN BELİRLE -> OK basınca klavye aç/kapat -> Doldurunca Aşağı Ok
                        // 2. GİZLİ SORU -> OK basınca klavye aç/kapat -> Doldurunca Aşağı Ok
                        // 3. CEVAP -> OK basınca klavye aç/kapat -> Doldurunca Aşağı Ok
                        // 4. KAYDET BUTONU -> OK basınca tamamla
                        // ==========================================

                        // 1. KUTU: PIN BELİRLE
                        Box(modifier = Modifier.bringIntoViewRequester(pinBringRequester)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "1. PIN Belirle (4 Rakam)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPinFocused) Color(0xFF42A5F5) else Color.LightGray
                                    )
                                    if (isPinFocused) {
                                        Text(
                                            text = if (isImeVisible) "[OK] Klavyeyi Kapat" else "[OK] Klavye Aç  [▼] Aşağı İt",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = newPin,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() }
                                        if (filtered.length <= 4) {
                                            newPin = filtered
                                            errorMessage = null
                                            if (filtered.length == 4) {
                                                // 4 hane dolduğunda klavyeyi otomatik kapat
                                                keyboardController?.hide()
                                            }
                                        }
                                    },
                                    placeholder = { Text("4 Haneli PIN (Örn: 1234)", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboardController?.hide() },
                                        onNext = {
                                            keyboardController?.hide()
                                            questionFocusRequester.requestFocus()
                                            coroutineScope.launch { questionBringRequester.bringIntoView() }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF42A5F5),
                                        unfocusedBorderColor = Color(0xFF444455),
                                        focusedContainerColor = Color(0xFF26263B),
                                        unfocusedContainerColor = Color(0xFF1B1B26)
                                    ),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { passwordVisible = !passwordVisible },
                                                modifier = Modifier.size(30.dp).focusable(false)
                                            ) {
                                                Icon(
                                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (isImeVisible) keyboardController?.hide() else keyboardController?.show()
                                                },
                                                modifier = Modifier.size(30.dp).focusable(false)
                                            ) {
                                                Icon(
                                                    if (isImeVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                                    contentDescription = null,
                                                    tint = if (isPinFocused) Color(0xFF42A5F5) else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp, max = 48.dp)
                                        .focusProperties {
                                            down = questionFocusRequester
                                        }
                                        .focusRequester(pinFocusRequester)
                                        .onFocusChanged { isPinFocused = it.isFocused }
                                        .border(
                                            width = if (isPinFocused) 2.5.dp else 0.dp,
                                            color = if (isPinFocused) Color(0xFF42A5F5) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                val nativeCode = event.nativeKeyEvent.keyCode
                                                val isDown = event.key == Key.DirectionDown || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                                                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter ||
                                                        nativeCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeCode == android.view.KeyEvent.KEYCODE_ENTER

                                                if (isDown) {
                                                    keyboardController?.hide()
                                                    questionFocusRequester.requestFocus()
                                                    coroutineScope.launch { questionBringRequester.bringIntoView() }
                                                    true
                                                } else if (isOk) {
                                                    if (isImeVisible) {
                                                        keyboardController?.hide()
                                                    } else {
                                                        keyboardController?.show()
                                                    }
                                                    true
                                                } else false
                                            } else false
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. KUTU: GİZLİ SORU BELİRLEME
                        Box(modifier = Modifier.bringIntoViewRequester(questionBringRequester)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "2. Gizli Soru Belirleme",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isQuestionFocused) Color(0xFF42A5F5) else Color.LightGray
                                    )
                                    if (isQuestionFocused) {
                                        Text(
                                            text = if (isImeVisible) "[OK] Klavyeyi Kapat" else "[OK] Klavye Aç  [▼] Aşağı İt",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = questionInput,
                                    onValueChange = {
                                        questionInput = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Örn: İlk okulunuzun adı?", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboardController?.hide() },
                                        onNext = {
                                            keyboardController?.hide()
                                            answerFocusRequester.requestFocus()
                                            coroutineScope.launch { answerBringRequester.bringIntoView() }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF42A5F5),
                                        unfocusedBorderColor = Color(0xFF444455),
                                        focusedContainerColor = Color(0xFF26263B),
                                        unfocusedContainerColor = Color(0xFF1B1B26)
                                    ),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (isImeVisible) keyboardController?.hide() else keyboardController?.show()
                                            },
                                            modifier = Modifier.size(30.dp).focusable(false)
                                        ) {
                                            Icon(
                                                if (isImeVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                                contentDescription = null,
                                                tint = if (isQuestionFocused) Color(0xFF42A5F5) else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp, max = 48.dp)
                                        .focusProperties {
                                            up = pinFocusRequester
                                            down = answerFocusRequester
                                        }
                                        .focusRequester(questionFocusRequester)
                                        .onFocusChanged { isQuestionFocused = it.isFocused }
                                        .border(
                                            width = if (isQuestionFocused) 2.5.dp else 0.dp,
                                            color = if (isQuestionFocused) Color(0xFF42A5F5) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                val nativeCode = event.nativeKeyEvent.keyCode
                                                val isDown = event.key == Key.DirectionDown || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                                                val isUp = event.key == Key.DirectionUp || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter ||
                                                        nativeCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeCode == android.view.KeyEvent.KEYCODE_ENTER

                                                if (isDown) {
                                                    keyboardController?.hide()
                                                    answerFocusRequester.requestFocus()
                                                    coroutineScope.launch { answerBringRequester.bringIntoView() }
                                                    true
                                                } else if (isUp) {
                                                    keyboardController?.hide()
                                                    pinFocusRequester.requestFocus()
                                                    coroutineScope.launch { pinBringRequester.bringIntoView() }
                                                    true
                                                } else if (isOk) {
                                                    if (isImeVisible) {
                                                        keyboardController?.hide()
                                                    } else {
                                                        keyboardController?.show()
                                                    }
                                                    true
                                                } else false
                                            } else false
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. KUTU: CEVAP BELİRLEME
                        Box(modifier = Modifier.bringIntoViewRequester(answerBringRequester)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "3. Gizli Soru Cevabı",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isAnswerFocused) Color(0xFF42A5F5) else Color.LightGray
                                    )
                                    if (isAnswerFocused) {
                                        Text(
                                            text = if (isImeVisible) "[OK] Klavyeyi Kapat" else "[OK] Klavye Aç  [▼] Kaydet'e Git",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF42A5F5)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = answerInput,
                                    onValueChange = {
                                        answerInput = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("Cevabınızı girin", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            saveButtonFocusRequester.requestFocus()
                                            coroutineScope.launch { saveBringRequester.bringIntoView() }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF42A5F5),
                                        unfocusedBorderColor = Color(0xFF444455),
                                        focusedContainerColor = Color(0xFF26263B),
                                        unfocusedContainerColor = Color(0xFF1B1B26)
                                    ),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (isImeVisible) keyboardController?.hide() else keyboardController?.show()
                                            },
                                            modifier = Modifier.size(30.dp).focusable(false)
                                        ) {
                                            Icon(
                                                if (isImeVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                                contentDescription = null,
                                                tint = if (isAnswerFocused) Color(0xFF42A5F5) else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp, max = 48.dp)
                                        .focusProperties {
                                            up = questionFocusRequester
                                            down = saveButtonFocusRequester
                                        }
                                        .focusRequester(answerFocusRequester)
                                        .onFocusChanged { isAnswerFocused = it.isFocused }
                                        .border(
                                            width = if (isAnswerFocused) 2.5.dp else 0.dp,
                                            color = if (isAnswerFocused) Color(0xFF42A5F5) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                val nativeCode = event.nativeKeyEvent.keyCode
                                                val isDown = event.key == Key.DirectionDown || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                                                val isUp = event.key == Key.DirectionUp || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter ||
                                                        nativeCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeCode == android.view.KeyEvent.KEYCODE_ENTER

                                                if (isDown) {
                                                    keyboardController?.hide()
                                                    saveButtonFocusRequester.requestFocus()
                                                    coroutineScope.launch { saveBringRequester.bringIntoView() }
                                                    true
                                                } else if (isUp) {
                                                    keyboardController?.hide()
                                                    questionFocusRequester.requestFocus()
                                                    coroutineScope.launch { questionBringRequester.bringIntoView() }
                                                    true
                                                } else if (isOk) {
                                                    if (isImeVisible) {
                                                        keyboardController?.hide()
                                                    } else {
                                                        keyboardController?.show()
                                                    }
                                                    true
                                                } else false
                                            } else false
                                        }
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // AŞAĞI BUTONLAR: KAYDET & İPTAL
                        Box(modifier = Modifier.bringIntoViewRequester(saveBringRequester)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        if (newPin.length != 4) {
                                            errorMessage = "PIN tam 4 haneli rakam olmalıdır."
                                            pinFocusRequester.requestFocus()
                                        } else if (questionInput.isBlank()) {
                                            errorMessage = "Lütfen bir gizli soru belirleyin."
                                            questionFocusRequester.requestFocus()
                                        } else if (answerInput.isBlank()) {
                                            errorMessage = "Lütfen gizli sorunun cevabını yazın."
                                            answerFocusRequester.requestFocus()
                                        } else {
                                            ParentalControlManager.setupPin(newPin, questionInput.trim(), answerInput.trim())
                                            Toast.makeText(context, "PIN ve gizli soru başarıyla kaydedildi.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSaveButtonFocused) Color(0xFF43A047) else Color(0xFF2E7D32)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(42.dp)
                                        .focusProperties {
                                            up = answerFocusRequester
                                            right = cancelButtonFocusRequester
                                        }
                                        .focusRequester(saveButtonFocusRequester)
                                        .onFocusChanged { isSaveButtonFocused = it.isFocused }
                                        .border(
                                            width = if (isSaveButtonFocused) 2.5.dp else 0.dp,
                                            color = if (isSaveButtonFocused) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                val nativeCode = event.nativeKeyEvent.keyCode
                                                val isUp = event.key == Key.DirectionUp || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                                val isRight = event.key == Key.DirectionRight || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT

                                                if (isUp) {
                                                    answerFocusRequester.requestFocus()
                                                    coroutineScope.launch { answerBringRequester.bringIntoView() }
                                                    true
                                                } else if (isRight) {
                                                    cancelButtonFocusRequester.requestFocus()
                                                    true
                                                } else false
                                            } else false
                                        }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kaydet & Tamamla", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(42.dp)
                                        .focusProperties {
                                            up = answerFocusRequester
                                            left = saveButtonFocusRequester
                                        }
                                        .focusRequester(cancelButtonFocusRequester)
                                        .onFocusChanged { isCancelButtonFocused = it.isFocused }
                                        .border(
                                            width = if (isCancelButtonFocused) 2.5.dp else 0.dp,
                                            color = if (isCancelButtonFocused) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown) {
                                                val nativeCode = event.nativeKeyEvent.keyCode
                                                val isUp = event.key == Key.DirectionUp || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_UP
                                                val isLeft = event.key == Key.DirectionLeft || nativeCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT

                                                if (isUp) {
                                                    answerFocusRequester.requestFocus()
                                                    coroutineScope.launch { answerBringRequester.bringIntoView() }
                                                    true
                                                } else if (isLeft) {
                                                    saveButtonFocusRequester.requestFocus()
                                                    true
                                                } else false
                                            } else false
                                        }
                                ) {
                                    Text(stringResource(R.string.close_desc), fontSize = 13.sp)
                                }
                            }
                        }

                    } else if (isRemoveMode) {
                        // ==========================================
                        // MOD 2: PIN Kaldırma
                        // ==========================================
                        if (removeStep == 1) {
                            Text(
                                text = "Mevcut PIN'inizi Girin",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputPin,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() }
                                    if (filtered.length <= 4) {
                                        inputPin = filtered
                                        inputError = false
                                    }
                                },
                                placeholder = { Text("4 Haneli PIN", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                isError = inputError,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    saveButtonFocusRequester.requestFocus()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF42A5F5),
                                    unfocusedBorderColor = Color(0xFF444455),
                                    focusedContainerColor = Color(0xFF26263B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp, max = 48.dp)
                                    .focusProperties { down = saveButtonFocusRequester }
                                    .focusRequester(pinFocusRequester)
                                    .onFocusChanged { isPinFocused = it.isFocused }
                                    .border(if (isPinFocused) 2.5.dp else 0.dp, Color(0xFF42A5F5), RoundedCornerShape(8.dp))
                            )
                            if (inputError) {
                                Text(stringResource(R.string.parental_wrong_pin), color = Color(0xFFEF5350), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                        } else {
                            Text(text = "Soru: ${secretQuestion ?: ""}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = answerInput,
                                onValueChange = { answerInput = it; inputError = false },
                                placeholder = { Text("Gizli cevabı girin", color = Color.Gray, fontSize = 12.sp) },
                                singleLine = true,
                                isError = inputError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    keyboardController?.hide()
                                    saveButtonFocusRequester.requestFocus()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF42A5F5),
                                    focusedContainerColor = Color(0xFF26263B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp, max = 48.dp)
                                    .focusProperties { down = saveButtonFocusRequester }
                                    .focusRequester(answerFocusRequester)
                                    .onFocusChanged { isAnswerFocused = it.isFocused }
                                    .border(if (isAnswerFocused) 2.5.dp else 0.dp, Color(0xFF42A5F5), RoundedCornerShape(8.dp))
                            )
                            if (inputError) {
                                Text(stringResource(R.string.parental_wrong_answer), color = Color(0xFFEF5350), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    if (removeStep == 1) {
                                        if (inputPin == currentPin) {
                                            ParentalControlManager.removePin()
                                            Toast.makeText(context, "PIN koruması kaldırıldı.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            inputError = true
                                            removeStep = 2
                                            answerInput = ""
                                            Toast.makeText(context, "Hatalı PIN! Gizli soruyu yanıtlayın.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (answerInput.trim().equals(secretAnswer?.trim(), ignoreCase = true)) {
                                            ParentalControlManager.removePin()
                                            Toast.makeText(context, "PIN koruması kaldırıldı.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            SupportRepository.createTicket(
                                                title = "PIN Remove Request",
                                                channelName = "Master Admin",
                                                message = "User failed PIN and secret answer."
                                            )
                                            Toast.makeText(context, context.getString(R.string.parental_support_sent), Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusRequester(saveButtonFocusRequester)
                                    .onFocusChanged { isSaveButtonFocused = it.isFocused }
                                    .border(if (isSaveButtonFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text(if (removeStep == 1) "Onayla" else "PIN Kaldır", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusRequester(cancelButtonFocusRequester)
                                    .onFocusChanged { isCancelButtonFocused = it.isFocused }
                                    .border(if (isCancelButtonFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text(stringResource(R.string.close_desc), color = Color.LightGray, fontSize = 13.sp)
                            }
                        }

                    } else if (isResetMode) {
                        // ==========================================
                        // MOD 3: PIN Sıfırlama (Gizli Soru + Yeni PIN)
                        // ==========================================
                        Text(text = "Soru: ${secretQuestion ?: ""}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        Text("1. Gizli Cevabınız", fontSize = 11.sp, color = if (isAnswerFocused) Color(0xFF42A5F5) else Color.LightGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = answerInput,
                            onValueChange = { answerInput = it; inputError = false },
                            placeholder = { Text("Cevabı girin", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            isError = inputError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                pinFocusRequester.requestFocus()
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF42A5F5),
                                focusedContainerColor = Color(0xFF26263B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp, max = 48.dp)
                                .focusProperties { down = pinFocusRequester }
                                .focusRequester(answerFocusRequester)
                                .onFocusChanged { isAnswerFocused = it.isFocused }
                                .border(if (isAnswerFocused) 2.5.dp else 0.dp, Color(0xFF42A5F5), RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("2. Yeni 4 Haneli PIN", fontSize = 11.sp, color = if (isPinFocused) Color(0xFF42A5F5) else Color.LightGray)
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = {
                                val filtered = it.filter { ch -> ch.isDigit() }
                                if (filtered.length <= 4) newPin = filtered
                            },
                            placeholder = { Text("Yeni PIN", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                saveButtonFocusRequester.requestFocus()
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF42A5F5),
                                focusedContainerColor = Color(0xFF26263B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp, max = 48.dp)
                                .focusProperties {
                                    up = answerFocusRequester
                                    down = saveButtonFocusRequester
                                }
                                .focusRequester(pinFocusRequester)
                                .onFocusChanged { isPinFocused = it.isFocused }
                                .border(if (isPinFocused) 2.5.dp else 0.dp, Color(0xFF42A5F5), RoundedCornerShape(8.dp))
                        )

                        if (inputError) {
                            Text(stringResource(R.string.parental_wrong_answer), color = Color(0xFFEF5350), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    keyboardController?.hide()
                                    if (answerInput.trim().equals(secretAnswer?.trim(), ignoreCase = true)) {
                                        if (newPin.length == 4) {
                                            ParentalControlManager.resetPin(newPin)
                                            Toast.makeText(context, "PIN başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Yeni PIN 4 haneli olmalıdır.", Toast.LENGTH_SHORT).show()
                                            pinFocusRequester.requestFocus()
                                        }
                                    } else {
                                        inputError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusProperties {
                                        up = pinFocusRequester
                                        right = cancelButtonFocusRequester
                                    }
                                    .focusRequester(saveButtonFocusRequester)
                                    .onFocusChanged { isSaveButtonFocused = it.isFocused }
                                    .border(if (isSaveButtonFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusProperties {
                                        up = pinFocusRequester
                                        left = saveButtonFocusRequester
                                    }
                                    .focusRequester(cancelButtonFocusRequester)
                                    .onFocusChanged { isCancelButtonFocused = it.isFocused }
                                    .border(if (isCancelButtonFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text(stringResource(R.string.close_desc), color = Color.LightGray, fontSize = 13.sp)
                            }
                        }

                    } else {
                        // ==========================================
                        // MOD 4: Mevcut PIN & Kilitli Kanallar Yönetimi
                        // "KALDIRMAK ICIN EBEBEYIN KATEGORUSUNDE KILITLI KANALLARIN LISTESI OLMALI KALDIRMAK ICIN ORDAN KALDIRILMALI"
                        // ==========================================

                        val lockedChannelList = remember(lockedChannels) {
                            ParentalControlManager.getLockedChannelInfoList()
                        }
                        val lockedGroupList = remember(lockedGroups) {
                            ParentalControlManager.getLockedGroupsList()
                        }

                        // KİLİTLİ İÇERİKLER LİSTESİ
                        Text(
                            text = "KİLİTLİ KANALLAR VE KATEGORİLER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF42A5F5)
                        )
                        Text(
                            text = "Aşağıdaki kilitleri kaldırmak için kumandadan 'Kilidi Kaldır' butonuna basın.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (lockedChannelList.isEmpty() && lockedGroupList.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF161622),
                                border = BorderStroke(1.dp, Color(0xFF2C2C40)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF66BB6A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Henüz kilitlenmiş bir içerik yok.",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Kanalları veya kategorileri kilitlemek için liste üzerinde kilit simgesine basın.",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. Kilitli Kategoriler
                                lockedGroupList.forEachIndexed { index, grp ->
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isItemFocused) Color(0xFF2E2436) else Color(0xFF1B1B26),
                                        border = BorderStroke(
                                            if (isItemFocused) 2.dp else 1.dp,
                                            if (isItemFocused) Color(0xFFFFB300) else Color(0xFF333348)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = grp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Kategori Kilidi",
                                                    color = Color(0xFFFFB300),
                                                    fontSize = 10.sp
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    ParentalControlManager.unlockGroup(grp)
                                                    Toast.makeText(context, "$grp kategorisinin kilidi kaldırıldı.", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isItemFocused) Color(0xFFD32F2F) else Color(0xFF8E2424)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                                    .onFocusChanged { isItemFocused = it.isFocused }
                                                    .border(
                                                        if (isItemFocused) 2.5.dp else 0.dp,
                                                        Color.White,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                            ) {
                                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kilidi Kaldır", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // 2. Kilitli Kanallar / Filmler / Diziler
                                lockedChannelList.forEachIndexed { index, item ->
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    val isFirstItem = lockedGroupList.isEmpty() && index == 0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isItemFocused) Color(0xFF2C2436) else Color(0xFF1B1B26),
                                        border = BorderStroke(
                                            if (isItemFocused) 2.dp else 1.dp,
                                            if (isItemFocused) Color(0xFF42A5F5) else Color(0xFF333348)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val icon = when (item.type.uppercase()) {
                                                "MOVIE" -> Icons.Default.Movie
                                                "SERIES" -> Icons.Default.Tv
                                                else -> Icons.Default.LiveTv
                                            }
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${item.group} • ${item.type}",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    ParentalControlManager.unlockChannel(item.url)
                                                    Toast.makeText(context, "${item.title} kilidi kaldırıldı.", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isItemFocused) Color(0xFFE53935) else Color(0xFF8E2424)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .then(if (isFirstItem) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                                    .onFocusChanged { isItemFocused = it.isFocused }
                                                    .border(
                                                        if (isItemFocused) 2.5.dp else 0.dp,
                                                        Color.White,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                            ) {
                                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Kilidi Kaldır", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PIN YÖNETİM BUTONLARI
                        var isResetBtnFocused by remember { mutableStateOf(false) }
                        var isRemoveBtnFocused by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isResetMode = true
                                    inputError = false
                                    answerInput = ""
                                    newPin = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isResetBtnFocused) Color(0xFF1E88E5) else Color(0xFF1565C0)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .then(if (lockedChannelList.isEmpty() && lockedGroupList.isEmpty()) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                    .focusRequester(resetPinButtonFocusRequester)
                                    .onFocusChanged { isResetBtnFocused = it.isFocused }
                                    .border(if (isResetBtnFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text("PIN Değiştir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    isRemoveMode = true
                                    removeStep = 1
                                    inputError = false
                                    inputPin = ""
                                    answerInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRemoveBtnFocused) Color(0xFFD32F2F) else Color(0xFF9E2424)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .onFocusChanged { isRemoveBtnFocused = it.isFocused }
                                    .border(if (isRemoveBtnFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                            ) {
                                Text("PIN Kaldır", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .focusRequester(cancelButtonFocusRequester)
                                .onFocusChanged { isCancelButtonFocused = it.isFocused }
                                .border(if (isCancelButtonFocused) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp))
                        ) {
                            Text(stringResource(R.string.close_desc), color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
