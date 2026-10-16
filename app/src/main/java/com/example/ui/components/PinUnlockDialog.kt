package com.example.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.ParentalControlManager
import kotlinx.coroutines.delay

@Composable
fun PinUnlockDialog(
    title: String? = null,
    subtitle: String? = null,
    onUnlock: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentPin by ParentalControlManager.pinCode.collectAsState()
    var inputPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val validPin = currentPin ?: ParentalControlManager.DEFAULT_PIN

    fun submitPin(pin: String) {
        if (pin == validPin) {
            onUnlock()
        } else {
            pinError = true
            inputPin = ""
        }
    }

    val defaultFocusRequester = remember { FocusRequester() }
    val unlockFocusRequester = remember { FocusRequester() }

    // Initial focus on the first number button
    LaunchedEffect(Unit) {
        delay(120)
        try {
            defaultFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // When 4 digits entered, automatically shift focus to the "Aç" button
    LaunchedEffect(inputPin) {
        if (inputPin.length == 4) {
            delay(60)
            try {
                unlockFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(12.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val nativeCode = event.nativeKeyEvent.keyCode
                        val unicodeChar = event.nativeKeyEvent.unicodeChar.toChar()

                        // 1. Direct Remote Number Keys (0-9, Numpad 0-9, Unicode digits)
                        val pressedDigit = when {
                            nativeCode in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 -> {
                                (nativeCode - AndroidKeyEvent.KEYCODE_0).toString()
                            }
                            nativeCode in AndroidKeyEvent.KEYCODE_NUMPAD_0..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> {
                                (nativeCode - AndroidKeyEvent.KEYCODE_NUMPAD_0).toString()
                            }
                            unicodeChar in '0'..'9' -> unicodeChar.toString()
                            event.key == Key.Zero || event.key == Key.NumPad0 -> "0"
                            event.key == Key.One || event.key == Key.NumPad1 -> "1"
                            event.key == Key.Two || event.key == Key.NumPad2 -> "2"
                            event.key == Key.Three || event.key == Key.NumPad3 -> "3"
                            event.key == Key.Four || event.key == Key.NumPad4 -> "4"
                            event.key == Key.Five || event.key == Key.NumPad5 -> "5"
                            event.key == Key.Six || event.key == Key.NumPad6 -> "6"
                            event.key == Key.Seven || event.key == Key.NumPad7 -> "7"
                            event.key == Key.Eight || event.key == Key.NumPad8 -> "8"
                            event.key == Key.Nine || event.key == Key.NumPad9 -> "9"
                            else -> null
                        }

                        if (pressedDigit != null) {
                            if (inputPin.length < 4) {
                                inputPin += pressedDigit
                                pinError = false
                            }
                            return@onPreviewKeyEvent true
                        }

                        // 2. Backspace / Delete
                        val isDelete = event.key == Key.Backspace || event.key == Key.Delete ||
                                nativeCode == AndroidKeyEvent.KEYCODE_DEL || nativeCode == AndroidKeyEvent.KEYCODE_FORWARD_DEL
                        if (isDelete) {
                            if (inputPin.isNotEmpty()) {
                                inputPin = inputPin.dropLast(1)
                                pinError = false
                            }
                            return@onPreviewKeyEvent true
                        }

                        // 3. Enter key
                        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter ||
                                nativeCode == AndroidKeyEvent.KEYCODE_ENTER || nativeCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
                        if (isEnter) {
                            if (inputPin.length == 4) {
                                submitPin(inputPin)
                                return@onPreviewKeyEvent true
                            }
                        }

                        // 4. Back / Escape
                        val isBack = event.key == Key.Back || event.key == Key.Escape || nativeCode == AndroidKeyEvent.KEYCODE_BACK
                        if (isBack) {
                            onDismiss()
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                },
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = maxWidth >= 480.dp || maxWidth > maxHeight

            // Card container
            Surface(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = if (isLandscape) 580.dp else 380.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161624),
                border = BorderStroke(1.5.dp, Color(0xFF2F2F48)),
                tonalElevation = 10.dp
            ) {
                com.example.util.KeepSystemBarsHidden()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE53935).copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title?.ifBlank { "Kilitli İçerik" } ?: stringResource(R.string.parental_enter_pin),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitle ?: "Kanalı açmak için PIN girin",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9EBA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLandscape) {
                        // TV / Landscape 2-column layout (Guaranteed visible 0 and no vertical overflow)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Panel: PIN dots, Status, Aç button, Kapat button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // 4 PIN Digit Dots
                                PinDotsRow(inputPin = inputPin, pinError = pinError)

                                Spacer(modifier = Modifier.height(8.dp))

                                // Status message
                                if (pinError) {
                                    Text(
                                        text = "Hatalı PIN! Tekrar deneyin.",
                                        color = Color(0xFFEF5350),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                } else if (currentPin == null) {
                                    Text(
                                        text = "Varsayılan PIN: 0000",
                                        color = Color(0xFFFFB300),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = if (inputPin.length == 4) "Kanalı açmak için [Aç]'a basın" else "Kumandadan 4 hane girin",
                                        color = if (inputPin.length == 4) Color(0xFF66BB6A) else Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = if (inputPin.length == 4) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Aç Button
                                val isReady = inputPin.length == 4
                                var isUnlockFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { submitPin(inputPin) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isReady || isUnlockFocused) Color(0xFF2E7D32) else Color(0xFF1E2822),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .focusRequester(unlockFocusRequester)
                                        .onFocusChanged { isUnlockFocused = it.isFocused }
                                        .border(
                                            width = if (isUnlockFocused) 2.5.dp else if (isReady) 1.5.dp else 1.dp,
                                            color = if (isUnlockFocused) Color.White else if (isReady) Color(0xFF66BB6A) else Color(0xFF2E4B33),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isReady || isUnlockFocused) Color.White else Color(0xFF81C784)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Kanalı Aç",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isReady || isUnlockFocused) Color.White else Color(0xFFB0BEC5)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                                ) {
                                    Text("Vazgeç", fontSize = 11.sp)
                                }
                            }

                            // Right Panel: 3x4 TV Keypad with 0 clearly visible on Row 4
                            Column(
                                modifier = Modifier.weight(1.15f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                KeypadGrid(
                                    inputPin = inputPin,
                                    onDigit = { d ->
                                        if (inputPin.length < 4) {
                                            inputPin += d
                                            pinError = false
                                        }
                                    },
                                    onDelete = {
                                        if (inputPin.isNotEmpty()) {
                                            inputPin = inputPin.dropLast(1)
                                            pinError = false
                                        }
                                    },
                                    onSubmit = {
                                        submitPin(inputPin)
                                    },
                                    defaultFocusRequester = defaultFocusRequester
                                )
                            }
                        }
                    } else {
                        // Portrait / Compact layout
                        PinDotsRow(inputPin = inputPin, pinError = pinError)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (pinError) {
                            Text(
                                text = "Hatalı PIN! Tekrar deneyin.",
                                color = Color(0xFFEF5350),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (currentPin == null) {
                            Text(
                                text = "Varsayılan PIN: 0000",
                                color = Color(0xFFFFB300),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        KeypadGrid(
                            inputPin = inputPin,
                            onDigit = { d ->
                                if (inputPin.length < 4) {
                                    inputPin += d
                                    pinError = false
                                }
                            },
                            onDelete = {
                                if (inputPin.isNotEmpty()) {
                                    inputPin = inputPin.dropLast(1)
                                    pinError = false
                                }
                            },
                            onSubmit = {
                                submitPin(inputPin)
                            },
                            defaultFocusRequester = defaultFocusRequester
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                        ) {
                            Text("Kapat", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDotsRow(inputPin: String, pinError: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val isFilled = i < inputPin.length
            val isActive = i == inputPin.length
            Box(
                modifier = Modifier
                    .size(38.dp, 42.dp)
                    .background(
                        color = if (pinError) Color(0xFF3E1E24)
                        else if (isActive) Color(0xFF262A3C)
                        else Color(0xFF212130),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (pinError) 2.dp else if (isActive) 2.dp else 1.dp,
                        color = if (pinError) Color(0xFFEF5350)
                        else if (isActive) Color(0xFF42A5F5)
                        else Color(0xFF38384E),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .background(
                                if (pinError) Color(0xFFEF5350) else Color.White,
                                CircleShape
                            )
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF42A5F5), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadGrid(
    inputPin: String,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    defaultFocusRequester: FocusRequester
) {
    // 3 columns x 4 rows
    // Row 1: 1, 2, 3
    // Row 2: 4, 5, 6
    // Row 3: 7, 8, 9
    // Row 4: DEL, 0, OK
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("DEL", "0", "OK")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        keys.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEachIndexed { colIndex, key ->
                    var isKeyFocused by remember { mutableStateOf(false) }
                    val isReadyToOpen = inputPin.length == 4 && key == "OK"

                    val buttonColor = when (key) {
                        "OK" -> if (isKeyFocused || isReadyToOpen) Color(0xFF388E3C) else Color(0xFF1B4D20)
                        "DEL" -> if (isKeyFocused) Color(0xFF5E35B1) else Color(0xFF322744)
                        "0" -> if (isKeyFocused) Color(0xFF1E88E5) else Color(0xFF26263C)
                        else -> if (isKeyFocused) Color(0xFF1E88E5) else Color(0xFF222236)
                    }

                    Button(
                        onClick = {
                            when (key) {
                                "DEL" -> onDelete()
                                "OK" -> onSubmit()
                                else -> onDigit(key)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .then(
                                if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(defaultFocusRequester)
                                else Modifier
                            )
                            .onFocusChanged { isKeyFocused = it.isFocused }
                            .border(
                                width = if (isKeyFocused) 2.5.dp else 1.dp,
                                color = if (isKeyFocused) Color.White else if (isReadyToOpen) Color(0xFF81C784) else Color(0xFF363650),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        when (key) {
                            "DEL" -> {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "Sil",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            "OK" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "Aç",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = key,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
