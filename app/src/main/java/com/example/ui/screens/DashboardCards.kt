package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.key.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.parser.M3uItem
import com.example.ui.theme.RedPrimary
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modifier supporting both Android TV remote D-Pad Center long-press (and shortcut keys like MENU/YELLOW)
 * as well as touch/mouse long-press gestures.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tvRemoteCardClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLockToggle: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var isLongPressHandled by remember { mutableStateOf(false) }
    var keyDownTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            longPressJob?.cancel()
            longPressJob = null
            isLongPressHandled = false
            keyDownTime = 0L
        }
    }

    return this
        .onFocusChanged { focusState ->
            if (!focusState.isFocused) {
                longPressJob?.cancel()
                longPressJob = null
                isLongPressHandled = false
                keyDownTime = 0L
            }
        }
        .onPreviewKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val keyCode = native.keyCode

            // Check if key is DPAD Center / Enter / Select / Button A
            val isCenterKey = keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                    keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
                    keyCode == AndroidKeyEvent.KEYCODE_BUTTON_A ||
                    keyCode == AndroidKeyEvent.KEYCODE_BUTTON_SELECT

            // Shortcut keys for favorites: Menu, F, Y, Bookmark, Yellow button, Star, Window
            val isFavoriteShortcut = keyCode == AndroidKeyEvent.KEYCODE_MENU ||
                    keyCode == AndroidKeyEvent.KEYCODE_F ||
                    keyCode == AndroidKeyEvent.KEYCODE_Y ||
                    keyCode == AndroidKeyEvent.KEYCODE_BOOKMARK ||
                    keyCode == AndroidKeyEvent.KEYCODE_PROG_YELLOW ||
                    keyCode == AndroidKeyEvent.KEYCODE_WINDOW ||
                    keyCode == AndroidKeyEvent.KEYCODE_STAR

            // Shortcut keys for lock: L, G, Green button
            val isLockShortcut = keyCode == AndroidKeyEvent.KEYCODE_L ||
                    keyCode == AndroidKeyEvent.KEYCODE_G ||
                    keyCode == AndroidKeyEvent.KEYCODE_PROG_GREEN

            // Shortcut keys for info / EPG / IMDb:
            // Remote Blue Button (PROG_BLUE = 186), Info Button (INFO = 165), Guide/EPG Button (GUIDE = 172), Key.I (37), Help (259), B key (30)
            val isInfoShortcut = keyCode == AndroidKeyEvent.KEYCODE_PROG_BLUE ||
                    keyCode == AndroidKeyEvent.KEYCODE_INFO ||
                    keyCode == AndroidKeyEvent.KEYCODE_GUIDE ||
                    keyCode == AndroidKeyEvent.KEYCODE_I ||
                    keyCode == AndroidKeyEvent.KEYCODE_HELP ||
                    keyCode == AndroidKeyEvent.KEYCODE_B

            if (isFavoriteShortcut) {
                if (keyEvent.type == KeyEventType.KeyUp) {
                    onLongClick()
                    return@onPreviewKeyEvent true
                }
                return@onPreviewKeyEvent true
            }

            if (isLockShortcut && onLockToggle != null) {
                if (keyEvent.type == KeyEventType.KeyUp) {
                    onLockToggle()
                    return@onPreviewKeyEvent true
                }
                return@onPreviewKeyEvent true
            }

            if (isInfoShortcut && onInfoClick != null) {
                if (keyEvent.type == KeyEventType.KeyUp) {
                    onInfoClick()
                    return@onPreviewKeyEvent true
                }
                return@onPreviewKeyEvent true
            }

            if (isCenterKey) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (isLongPressHandled) {
                        return@onPreviewKeyEvent true
                    }
                    if (native.repeatCount == 0) {
                        isLongPressHandled = false
                        keyDownTime = System.currentTimeMillis()
                        longPressJob?.cancel()
                        longPressJob = coroutineScope.launch {
                            delay(350L)
                            if (!isLongPressHandled) {
                                isLongPressHandled = true
                                onLongClick()
                            }
                        }
                        // Allow initial KeyDown to pass for ripple & visual focus
                        return@onPreviewKeyEvent false
                    } else {
                        // Key is being held down on TV remote (repeatCount >= 1 or isLongPress)
                        longPressJob?.cancel()
                        longPressJob = null
                        if (!isLongPressHandled) {
                            isLongPressHandled = true
                            onLongClick()
                        }
                        return@onPreviewKeyEvent true
                    }
                } else if (keyEvent.type == KeyEventType.KeyUp) {
                    longPressJob?.cancel()
                    longPressJob = null
                    val pressDuration = if (keyDownTime > 0) System.currentTimeMillis() - keyDownTime else 0L
                    keyDownTime = 0L

                    if (isLongPressHandled || pressDuration >= 350L) {
                        if (!isLongPressHandled) {
                            isLongPressHandled = true
                            onLongClick()
                        }
                        isLongPressHandled = false
                        // CONSUME KeyUp so clickable / combinedClickable DOES NOT trigger onClick!
                        return@onPreviewKeyEvent true
                    }
                    // Short press: let clickable / combinedClickable handle normal click
                    return@onPreviewKeyEvent false
                }
            }
            false
        }
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
}

@Composable
fun HeroBanner(
    item: M3uItem,
    onClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showInfoDialog by remember { mutableStateOf(false) }

    val isSeries = item.type == com.example.parser.ItemType.SERIES ||
        !item.seriesName.isNullOrBlank() ||
        Regex("(?i)\\b(s\\d{1,2}\\s*e\\d{1,3}|sezon\\s*\\d+|b[öo]l[üu]m\\s*\\d+)\\b").containsMatchIn(item.title)
    val queryTitle = if (isSeries) {
        item.seriesName?.takeIf { it.isNotBlank() } ?: item.title
    } else {
        item.title
    }

    var backdropList by remember(item.url, item.title) { mutableStateOf<List<String>>(emptyList()) }
    var currentBackdropIndex by remember(item.url, item.title) { mutableIntStateOf(0) }

    LaunchedEffect(queryTitle, isSeries, item.url) {
        currentBackdropIndex = 0
        val detail = fetchTmdbFullDetail(query = queryTitle, isSeries = isSeries)
        val itemBackdrops = detail?.backdropUrls?.filter { it.isNotBlank() }.orEmpty()
        if (itemBackdrops.size > 1) {
            backdropList = itemBackdrops
        } else {
            val fallbackBackdrops = fetchTmdbFallbackBackdrops("969681")
            backdropList = when {
                itemBackdrops.isNotEmpty() && fallbackBackdrops.isNotEmpty() -> (itemBackdrops + fallbackBackdrops).distinct()
                fallbackBackdrops.isNotEmpty() -> fallbackBackdrops
                itemBackdrops.isNotEmpty() -> itemBackdrops
                !item.logo.isNullOrBlank() -> listOf(item.logo)
                else -> emptyList()
            }
        }
    }

    LaunchedEffect(backdropList) {
        currentBackdropIndex = 0
        if (backdropList.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(3500L)
                currentBackdropIndex = (currentBackdropIndex + 1) % backdropList.size
            }
        }
    }

    val activeBackdropUrl = backdropList.getOrNull(currentBackdropIndex) ?: item.logo
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val handleToggleFavorite = {
        val added = com.example.model.FavoritesManager.toggleFavorite(item.url)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (added) "Favorilere eklendi: ${item.title}" else "Favorilerden çıkarıldı: ${item.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(3.5.dp, RedPrimary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .tvRemoteCardClickable(
                onClick = onClick,
                onLongClick = handleToggleFavorite
            )
            .background(Color(0xFF0F0F17))
    ) {
        val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
        val isFavorite = favoriteUrls.contains(item.url)
        val lockedChannels by com.example.model.ParentalControlManager.lockedChannels.collectAsState()
        val lockedGroups by com.example.model.ParentalControlManager.lockedGroups.collectAsState()
        val explicitlyUnlockedChannels by com.example.model.ParentalControlManager.explicitlyUnlockedChannels.collectAsState()
        val explicitlyUnlockedGroups by com.example.model.ParentalControlManager.explicitlyUnlockedGroups.collectAsState()
        val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
        val hasPin = com.example.model.ParentalControlManager.pinCode.collectAsState().value != null
        val isLocked = com.example.model.ParentalControlManager.isItemLocked(item)
        var showUnlockDialog by remember { mutableStateOf(false) }

        androidx.compose.animation.Crossfade(
            targetState = if (isLocked) null else activeBackdropUrl,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
            modifier = Modifier.fillMaxSize(),
            label = "hero_backdrop_slide"
        ) { bgUrl ->
            if (!bgUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(bgUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.62f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.48f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = if (isLocked) com.example.model.ParentalControlManager.maskTitle(item.title) else item.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.play), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            val added = com.example.model.FavoritesManager.toggleFavorite(item.url)
                            Toast.makeText(
                                context,
                                if (added) "Favorilere eklendi: ${item.title}" else "Favorilerden çıkarıldı: ${item.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) Color.Red else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val locked = com.example.model.ParentalControlManager.toggleChannelLock(
                                channelUrl = item.url,
                                title = item.title,
                                group = item.group,
                                type = item.type.name
                            )
                            Toast.makeText(
                                context,
                                if (locked) "Kilitlendi (Varsayılan PIN: 0000): ${item.title}" else "Kilit kaldırıldı: ${item.title}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Kilit",
                            tint = if (isLocked) Color(0xFFEF5350) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (onInfoClick != null) {
                                onInfoClick()
                            } else {
                                showInfoDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.info_button_desc),
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!item.logo.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .height(160.dp)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (showInfoDialog) {
            ContentInfoDialog(
                item = item,
                onDismiss = { showInfoDialog = false },
                onPlay = {
                    showInfoDialog = false
                    onClick()
                }
            )
        }
    }
}

@Composable
fun MovieCard(
    item: M3uItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(122.dp),
    badgeText: String? = null,
    onInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
    val favoriteMovieTitles by com.example.model.FavoritesManager.favoriteMovieTitles.collectAsState()
    val isFavorite = com.example.model.FavoritesManager.isMovieFavorite(item)
    val lockedChannels by com.example.model.ParentalControlManager.lockedChannels.collectAsState()
    val lockedGroups by com.example.model.ParentalControlManager.lockedGroups.collectAsState()
    val explicitlyUnlockedChannels by com.example.model.ParentalControlManager.explicitlyUnlockedChannels.collectAsState()
    val explicitlyUnlockedGroups by com.example.model.ParentalControlManager.explicitlyUnlockedGroups.collectAsState()
    val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
    val hasPin = com.example.model.ParentalControlManager.pinCode.collectAsState().value != null
    val isLocked = com.example.model.ParentalControlManager.isItemLocked(item)
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var logoLoadFailed by remember(item.logo) { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val handleToggleFavorite = {
        val added = com.example.model.FavoritesManager.toggleMovieFavorite(item)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (added) "Favorilere eklendi: ${item.title}" else "Favorilerden çıkarıldı: ${item.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleToggleLock = {
        val locked = com.example.model.ParentalControlManager.toggleChannelLock(
            channelUrl = item.url,
            title = item.title,
            group = item.group,
            type = item.type.name
        )
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (locked) "Kilitlendi (Varsayılan PIN: 0000): ${item.title}" else "Kilit kaldırıldı: ${item.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleInfoClick = {
        if (onInfoClick != null) {
            onInfoClick()
        } else {
            showInfoDialog = true
        }
    }

    Card(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(10.dp))
                else Modifier
            )
            .tvRemoteCardClickable(
                onClick = onClick,
                onLongClick = handleToggleFavorite,
                onLockToggle = handleToggleLock,
                onInfoClick = handleInfoClick
            ),
        shape = RoundedCornerShape(10.dp),
        border = if (isFocused) BorderStroke(3.dp, RedPrimary) else null,
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF262C3A) else Color(0xFF1B202A))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xFF080F19)),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF261922), CircleShape)
                            .border(2.dp, Color(0xFFEF5350).copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli İçerik",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (!item.logo.isNullOrBlank() && !logoLoadFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (item.type == com.example.parser.ItemType.LIVE) ContentScale.Fit else ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.img_app_icon),
                        error = painterResource(id = R.drawable.img_app_icon),
                        onError = { logoLoadFailed = true }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLocked) com.example.model.ParentalControlManager.maskTitle(item.title) else item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLocked) Color(0xFFB0BEC5) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = handleToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) Color.Red else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = handleToggleLock,
                        modifier = Modifier
                            .size(32.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Kilit",
                            tint = if (isLocked) Color(0xFFEF5350) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = handleInfoClick,
                        modifier = Modifier
                            .size(28.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.info_button_desc),
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        ContentInfoDialog(
            item = item,
            onDismiss = { showInfoDialog = false },
            onPlay = {
                showInfoDialog = false
                onClick()
            }
        )
    }
}

@Composable
fun LiveEpgDialog(
    channel: M3uItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
    val isFavorite = favoriteUrls.contains(channel.url.trim())
    var epgList by remember(channel.url) { mutableStateOf<List<com.example.model.EpgProgram>>(emptyList()) }
    var nowProgram by remember(channel.url) { mutableStateOf<com.example.model.EpgProgram?>(null) }
    var isLoading by remember(channel.url) { mutableStateOf(true) }

    LaunchedEffect(channel.url) {
        isLoading = true
        try {
            val list = com.example.model.EpgRepository.getFullEpg(channel)
            val (now, _) = com.example.model.EpgRepository.getNowNext(channel)
            epgList = list
            nowProgram = now ?: list.firstOrNull { it.startMs <= System.currentTimeMillis() && System.currentTimeMillis() < it.endMs }
        } catch (_: Exception) {}
        isLoading = false
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.util.KeepSystemBarsHidden()
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = if (isLandscape) 780.dp else 650.dp)
                .padding(vertical = if (isLandscape) 8.dp else 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10141E)),
            border = BorderStroke(1.5.dp, Color(0xFF29B6F6).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isLandscape) 14.dp else 18.dp)
            ) {
                // Header: Channel Logo + Channel Title + Group Badge + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isLandscape) 48.dp else 56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B2232))
                            .border(1.dp, Color(0xFF29B6F6).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!channel.logo.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(channel.logo)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = channel.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF29B6F6).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF29B6F6))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF29B6F6),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EPG YAYIN AKIŞI",
                                        color = Color(0xFF29B6F6),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!channel.group.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF232D3F),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = channel.group,
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = channel.title,
                            color = Color.White,
                            fontSize = if (isLandscape) 17.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF242C3D), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Content: Current program + Upcoming programs OR fallback message
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF29B6F6),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Yayın akışı bilgisi yükleniyor...",
                                color = Color(0xFF90CAF9),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (nowProgram != null || epgList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isLandscape) 280.dp else 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Currently Playing Program Card
                        val currentProg = nowProgram ?: epgList.firstOrNull()
                        if (currentProg != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                                border = BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = Color(0xFFE53935),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "CANLI YAYINDA",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (currentProg.startMs > 0L && currentProg.endMs > 0L) {
                                            Text(
                                                text = "${timeFormat.format(java.util.Date(currentProg.startMs))} - ${timeFormat.format(java.util.Date(currentProg.endMs))}",
                                                color = Color(0xFF40C4FF),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = currentProg.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    if (currentProg.startMs > 0L && currentProg.endMs > currentProg.startMs) {
                                        val nowMs = System.currentTimeMillis()
                                        val total = (currentProg.endMs - currentProg.startMs).toFloat()
                                        val elapsed = (nowMs - currentProg.startMs).toFloat().coerceIn(0f, total)
                                        val progress = if (total > 0f) elapsed / total else 0f

                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFF29B6F6),
                                            trackColor = Color(0xFF222F44)
                                        )
                                    }

                                    if (!currentProg.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentProg.description,
                                            color = Color(0xFFCFD8DC),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Upcoming programs
                        val upcoming = epgList.filter { it != currentProg && it.startMs >= (currentProg?.startMs ?: 0L) }
                        if (upcoming.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SIRADAKİ PROGRAMLAR",
                                color = Color(0xFF90CAF9),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            upcoming.take(8).forEach { prog ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .background(Color(0xFF131824), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (prog.startMs > 0L) {
                                        Surface(
                                            color = Color(0xFF1F283B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = timeFormat.format(java.util.Date(prog.startMs)),
                                                color = Color(0xFF81D4FA),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prog.title,
                                            color = Color(0xFFECEFF1),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (!prog.description.isNullOrBlank()) {
                                            Text(
                                                text = prog.description,
                                                color = Color(0xFF90A4AE),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback when stream provider doesn't supply EPG
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151A26)),
                        border = BorderStroke(1.dp, Color(0xFF263248))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Yayın Akışı (EPG) Bilgisi",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bu kanal için IPTV sağlayıcınız tarafından anlık EPG program rehberi iletilmemektedir.",
                                color = Color(0xFFB0BEC5),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF1E2638),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Kumanda Kısayolu: Mavi Tuş / Info tuşu ile tüm kanallarda EPG açabilirsiniz",
                                    color = Color(0xFF81D4FA),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons Row: Play Channel + Favorite + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onPlay()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kanalı İzle",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.model.FavoritesManager.toggleFavorite(channel.url)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, if (isFavorite) Color(0xFFEF5350) else Color(0xFF37474F)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color(0xFFEF5350) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFavorite) "Favoride" else "Favori",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF37474F)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Kapat",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelCard(
    item: M3uItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
    val isFavorite = favoriteUrls.contains(item.url.trim())
    val lockedChannels by com.example.model.ParentalControlManager.lockedChannels.collectAsState()
    val lockedGroups by com.example.model.ParentalControlManager.lockedGroups.collectAsState()
    val explicitlyUnlockedChannels by com.example.model.ParentalControlManager.explicitlyUnlockedChannels.collectAsState()
    val explicitlyUnlockedGroups by com.example.model.ParentalControlManager.explicitlyUnlockedGroups.collectAsState()
    val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
    val hasPin = com.example.model.ParentalControlManager.pinCode.collectAsState().value != null
    val isLocked = com.example.model.ParentalControlManager.isItemLocked(item)
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var logoLoadFailed by remember(item.logo) { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val handleToggleFavorite = {
        val added = com.example.model.FavoritesManager.toggleFavorite(item.url)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (added) "Favorilere eklendi: ${item.title}" else "Favorilerden çıkarıldı: ${item.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleToggleLock = {
        val locked = com.example.model.ParentalControlManager.toggleChannelLock(
            channelUrl = item.url,
            title = item.title,
            group = item.group,
            type = "LIVE"
        )
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (locked) "Kilitlendi (Varsayılan PIN: 0000): ${item.title}" else "Kilit kaldırıldı: ${item.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleInfoClick = {
        if (onInfoClick != null) {
            onInfoClick()
        } else {
            showInfoDialog = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(10.dp))
                else Modifier
            )
            .tvRemoteCardClickable(
                onClick = onClick,
                onLongClick = handleToggleFavorite,
                onLockToggle = handleToggleLock,
                onInfoClick = handleInfoClick
            ),
        shape = RoundedCornerShape(10.dp),
        border = if (isFocused) BorderStroke(3.dp, RedPrimary) else null,
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF262C3A) else Color(0xFF1B202A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF080F19)),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF261922), CircleShape)
                            .border(1.5.dp, Color(0xFFEF5350).copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli Kanal",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else if (!item.logo.isNullOrBlank() && !logoLoadFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(id = R.drawable.img_app_icon),
                        error = painterResource(id = R.drawable.img_app_icon),
                        onError = { logoLoadFailed = true }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isLocked) com.example.model.ParentalControlManager.maskTitle(item.title) else item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLocked) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = handleToggleFavorite,
                    modifier = Modifier
                        .size(32.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favori",
                        tint = if (isFavorite) Color.Red else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = handleToggleLock,
                    modifier = Modifier
                        .size(32.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Kilit",
                        tint = if (isLocked) Color(0xFFEF5350) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = handleInfoClick,
                    modifier = Modifier
                        .size(28.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.info_button_desc),
                        tint = Color(0xFF29B6F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    if (showInfoDialog) {
        if (item.type == com.example.parser.ItemType.LIVE) {
            LiveEpgDialog(
                channel = item,
                onDismiss = { showInfoDialog = false },
                onPlay = {
                    showInfoDialog = false
                    onClick()
                }
            )
        } else {
            ContentInfoDialog(
                item = item,
                onDismiss = { showInfoDialog = false },
                onPlay = {
                    showInfoDialog = false
                    onClick()
                }
            )
        }
    }
}

@Composable
fun SeriesCard(
    seriesName: String,
    item: M3uItem,
    episodeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    episodes: List<M3uItem> = emptyList(),
    onPlayEpisode: ((M3uItem) -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
    val favoriteSeriesNames by com.example.model.FavoritesManager.favoriteSeriesNames.collectAsState()
    val isFavorite = com.example.model.FavoritesManager.isSeriesFavorite(item, episodes)
    val lockedChannels by com.example.model.ParentalControlManager.lockedChannels.collectAsState()
    val lockedGroups by com.example.model.ParentalControlManager.lockedGroups.collectAsState()
    val explicitlyUnlockedChannels by com.example.model.ParentalControlManager.explicitlyUnlockedChannels.collectAsState()
    val explicitlyUnlockedGroups by com.example.model.ParentalControlManager.explicitlyUnlockedGroups.collectAsState()
    val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
    val hasPin = com.example.model.ParentalControlManager.pinCode.collectAsState().value != null
    val isLocked = com.example.model.ParentalControlManager.isItemLocked(item)
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var logoLoadFailed by remember(item.logo) { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val handleToggleFavorite = {
        val added = com.example.model.FavoritesManager.toggleSeriesFavorite(item, episodes)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (added) "Favorilere eklendi: $seriesName" else "Favorilerden çıkarıldı: $seriesName",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleToggleLock = {
        val locked = com.example.model.ParentalControlManager.toggleChannelLock(
            channelUrl = item.url,
            title = seriesName,
            group = item.group,
            type = "SERIES"
        )
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (locked) "Kilitlendi (Varsayılan PIN: 0000): $seriesName" else "Kilit kaldırıldı: $seriesName",
            Toast.LENGTH_SHORT
        ).show()
    }

    val handleInfoClick = {
        if (onInfoClick != null) {
            onInfoClick()
        } else {
            showInfoDialog = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(10.dp))
                else Modifier
            )
            .tvRemoteCardClickable(
                onClick = onClick,
                onLongClick = handleToggleFavorite,
                onLockToggle = handleToggleLock,
                onInfoClick = handleInfoClick
            ),
        shape = RoundedCornerShape(10.dp),
        border = if (isFocused) BorderStroke(3.dp, RedPrimary) else null,
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF262C3A) else Color(0xFF1B202A))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(Color(0xFF080F19)),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF261922), CircleShape)
                            .border(2.dp, Color(0xFFEF5350).copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilitli Dizi",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (!item.logo.isNullOrBlank() && !logoLoadFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = seriesName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.img_app_icon),
                        error = painterResource(id = R.drawable.img_app_icon),
                        onError = { logoLoadFailed = true }
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon),
                        contentDescription = seriesName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "$episodeCount Bölüm",
                        fontSize = 9.sp,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLocked) com.example.model.ParentalControlManager.maskTitle(seriesName) else seriesName,
                    fontSize = 11.sp,
                    color = if (isLocked) Color(0xFFB0BEC5) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = handleToggleFavorite,
                        modifier = Modifier
                            .size(32.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) Color.Red else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = handleToggleLock,
                        modifier = Modifier
                            .size(32.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Kilit",
                            tint = if (isLocked) Color(0xFFEF5350) else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = handleInfoClick,
                        modifier = Modifier
                            .size(28.dp)
                            .focusProperties { canFocus = false }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.info_button_desc),
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        ContentInfoDialog(
            item = item.copy(
                type = com.example.parser.ItemType.SERIES,
                seriesName = seriesName.ifBlank { item.seriesName ?: item.title }
            ),
            seriesEpisodes = episodes,
            onDismiss = { showInfoDialog = false },
            onPlay = { targetEpisode ->
                showInfoDialog = false
                if (targetEpisode != null && onPlayEpisode != null) {
                    onPlayEpisode(targetEpisode)
                } else {
                    onClick()
                }
            }
        )
    }
}

@Composable
fun ProgressCard(progress: com.example.model.db.PlaybackProgressEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var logoLoadFailed by remember(progress.logo) { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val handleToggleFavorite = {
        val added = com.example.model.FavoritesManager.toggleFavorite(progress.url)
        try {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
        Toast.makeText(
            context,
            if (added) "Favorilere eklendi: ${progress.title}" else "Favorilerden çıkarıldı: ${progress.title}",
            Toast.LENGTH_SHORT
        ).show()
    }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .tvRemoteCardClickable(
                onClick = onClick,
                onLongClick = handleToggleFavorite
            ),
        shape = RoundedCornerShape(8.dp),
        border = if (isFocused) BorderStroke(3.dp, RedPrimary) else null,
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF1B202A) else Color(0xFF080F19))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!progress.logo.isNullOrBlank() && !logoLoadFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(progress.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = progress.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.img_app_icon),
                    error = painterResource(id = R.drawable.img_app_icon),
                    onError = { logoLoadFailed = true }
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_app_icon),
                    contentDescription = progress.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 22.dp, top = 4.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = progress.title,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (progress.durationMs > 0) (progress.positionMs.toFloat() / progress.durationMs.toFloat()) else 0f },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun MatchFixtureCard(fixture: MatchFixture, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .width(220.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF28283E) else Color(0xFF1E1E2E)),
        border = BorderStroke(if (isFocused) 3.dp else 1.dp, if (isFocused || fixture.isLive) RedPrimary else Color(0xFF2E2E42))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fixture.league,
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (fixture.isLive) {
                    Surface(
                        color = Color(0xFFE50914),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "CANLI",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = fixture.dateText,
                        fontSize = 10.sp,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (fixture.homeLogo.isNotBlank()) {
                        AsyncImage(
                            model = fixture.homeLogo,
                            contentDescription = fixture.homeTeam,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fixture.homeTeam,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (fixture.homeScore.isNotBlank() && fixture.awayScore.isNotBlank()) {
                    Text(
                        text = "${fixture.homeScore} - ${fixture.awayScore}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (fixture.isLive) Color(0xFFFF5252) else Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        text = "VS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (fixture.awayLogo.isNotBlank()) {
                        AsyncImage(
                            model = fixture.awayLogo,
                            contentDescription = fixture.awayTeam,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fixture.awayTeam,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A3E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = fixture.broadcastChannel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MatchFixtureDetailedCard(fixture: MatchFixture, onWatchClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.03f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onWatchClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF242E3D) else Color(0xFF1B222D)),
        border = BorderStroke(if (isFocused) 3.dp else 1.dp, if (isFocused || fixture.isLive) RedPrimary else Color(0xFF2C384A))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fixture.league,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )
                }

                if (fixture.isLive) {
                    Surface(
                        color = Color(0xFFE50914),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CANLI",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = fixture.dateText,
                        fontSize = 11.sp,
                        color = Color(0xFFFFCA28),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (fixture.homeLogo.isNotBlank()) {
                        AsyncImage(
                            model = fixture.homeLogo,
                            contentDescription = fixture.homeTeam,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = fixture.homeTeam,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Score or VS
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (fixture.homeScore.isNotBlank() && fixture.awayScore.isNotBlank()) {
                        Text(
                            text = "${fixture.homeScore} - ${fixture.awayScore}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (fixture.isLive) Color(0xFFFF5252) else Color.White
                        )
                    } else {
                        Text(
                            text = if (fixture.isLive) "CANLI" else "VS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }

                // Away Team
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = fixture.awayTeam,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (fixture.awayLogo.isNotBlank()) {
                        AsyncImage(
                            model = fixture.awayLogo,
                            contentDescription = fixture.awayTeam,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Yayın: ${fixture.broadcastChannel}",
                        fontSize = 11.sp,
                        color = Color(0xFF90CAF9),
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onWatchClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (fixture.isLive) Color(0xFFE50914) else MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (fixture.isLive) "Yayını İzle" else "Kanala Git", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ImdbCard(
    item: ImdbUpcomingItem,
    isNotified: Boolean,
    onNotifyToggle: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .width(150.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF242436) else Color(0xFF1A1A28)),
        border = BorderStroke(if (isFocused) 3.dp else 1.dp, if (isFocused) RedPrimary else Color(0xFF2A2A3E))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.67f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_gallery)
                )

                if (item.trailerKey != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${item.trailerKey}"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "YouTube uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Fragman",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    color = if (item.type == "Film") Color(0xFF1E88E5) else Color(0xFF8E24AA),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.type,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFFF5C518),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.imdbRating,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.releaseDate,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onNotifyToggle,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNotified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isNotified) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isNotified) "Haber Ver" else "Haber Ver",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = if (item.trailerKey != null) Color(0xFFE50914) else Color.DarkGray,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (item.trailerKey != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${item.trailerKey}"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "YouTube uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Fragman bulunamadı", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.trailerKey != null) Icons.Default.PlayArrow else Icons.Default.PlayDisabled,
                            contentDescription = if (item.trailerKey != null) "Fragman İzle" else "Fragman Yok",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContentInfoDialog(
    item: M3uItem,
    onDismiss: () -> Unit,
    seriesEpisodes: List<M3uItem> = emptyList(),
    onPlay: ((M3uItem?) -> Unit)? = null
) {
    val isSeries = item.type == com.example.parser.ItemType.SERIES
    val displayTitle = if (isSeries) {
        item.seriesName?.takeIf { it.isNotBlank() } ?: item.title
    } else {
        item.title
    }
    val queryTitle = if (isSeries) {
        item.seriesName?.takeIf { it.isNotBlank() } ?: item.title
    } else {
        item.title
    }
    ContentInfoDialog(
        title = displayTitle,
        posterUrl = item.logo,
        isSeries = isSeries,
        onDismiss = onDismiss,
        searchQuery = queryTitle,
        mediaUrl = item.url,
        seriesEpisodes = seriesEpisodes,
        onPlay = onPlay
    )
}

@Composable
fun ContentInfoDialog(
    title: String,
    posterUrl: String?,
    isSeries: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String = title,
    tmdbId: String? = null,
    initialOverview: String? = null,
    initialRating: String? = null,
    initialReleaseDate: String? = null,
    genres: List<String> = emptyList(),
    mediaUrl: String? = null,
    seriesEpisodes: List<M3uItem> = emptyList(),
    onPlay: ((M3uItem?) -> Unit)? = null
) {
    val context = LocalContext.current
    var tmdbDetail by remember { mutableStateOf<TmdbFullDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var resumeProgress by remember { mutableStateOf<com.example.model.db.PlaybackProgressEntity?>(null) }
    var matchedResumeEpisode by remember { mutableStateOf<M3uItem?>(null) }
    var isOverviewExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, isSeries, tmdbId) {
        isLoading = true
        tmdbDetail = fetchTmdbFullDetail(
            query = searchQuery,
            isSeries = isSeries,
            tmdbId = tmdbId
        )
        isLoading = false
    }

    LaunchedEffect(mediaUrl, seriesEpisodes) {
        try {
            val dao = com.example.model.db.AppDatabase.getDatabase(context).iptvDao()
            var foundProgress: com.example.model.db.PlaybackProgressEntity? = null
            var foundEp: M3uItem? = null

            if (!mediaUrl.isNullOrBlank()) {
                val cached = com.example.model.PlayerRepository.lastPositions[mediaUrl]
                val direct = dao.getProgressForUrl(mediaUrl)
                if (cached != null && cached.first > 1000L) {
                    foundProgress = com.example.model.db.PlaybackProgressEntity(
                        url = mediaUrl,
                        title = title,
                        logo = posterUrl,
                        type = if (isSeries) "SERIES" else "MOVIE",
                        positionMs = cached.first,
                        durationMs = cached.second.coerceAtLeast(cached.first + 60_000L)
                    )
                } else if (direct != null && direct.positionMs > 0L && direct.durationMs > 0L) {
                    foundProgress = direct
                }
            }

            if (foundProgress == null && seriesEpisodes.isNotEmpty()) {
                var latestTimestamp = -1L
                for (ep in seriesEpisodes) {
                    if (ep.url.isNotBlank()) {
                        val cached = com.example.model.PlayerRepository.lastPositions[ep.url]
                        val p = dao.getProgressForUrl(ep.url)
                        if (cached != null && cached.first > 1000L) {
                            val ts = p?.timestamp ?: System.currentTimeMillis()
                            if (ts >= latestTimestamp) {
                                latestTimestamp = ts
                                foundProgress = com.example.model.db.PlaybackProgressEntity(
                                    url = ep.url,
                                    title = ep.title,
                                    logo = ep.logo ?: posterUrl,
                                    type = "SERIES",
                                    positionMs = cached.first,
                                    durationMs = cached.second.coerceAtLeast(cached.first + 60_000L),
                                    timestamp = ts
                                )
                                foundEp = ep
                            }
                        } else if (p != null && p.positionMs > 0L && p.durationMs > 0L && p.timestamp > latestTimestamp) {
                            latestTimestamp = p.timestamp
                            foundProgress = p
                            foundEp = ep
                        }
                    }
                }
            }

            resumeProgress = foundProgress
            matchedResumeEpisode = foundEp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val backdropList = remember(tmdbDetail, posterUrl) {
        val fromTmdb = tmdbDetail?.backdropUrls?.filter { it.isNotBlank() }.orEmpty()
        when {
            fromTmdb.isNotEmpty() -> fromTmdb
            !tmdbDetail?.backdropUrl.isNullOrBlank() -> listOf(tmdbDetail!!.backdropUrl!!)
            !tmdbDetail?.posterUrl.isNullOrBlank() -> listOf(tmdbDetail!!.posterUrl!!)
            !posterUrl.isNullOrBlank() -> listOf(posterUrl)
            else -> emptyList()
        }
    }
    var currentBackdropIndex by remember(backdropList) { mutableIntStateOf(0) }
    LaunchedEffect(backdropList) {
        currentBackdropIndex = 0
        if (backdropList.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(3500L)
                currentBackdropIndex = (currentBackdropIndex + 1) % backdropList.size
            }
        }
    }
    val backdropOrPoster = backdropList.getOrNull(currentBackdropIndex) ?: tmdbDetail?.backdropUrl ?: tmdbDetail?.posterUrl ?: posterUrl
    val ratingText = tmdbDetail?.rating?.takeIf { it > 0.0 }?.let {
        String.format(Locale.US, "%.1f", it)
    } ?: initialRating?.takeIf { it.isNotBlank() }
    val releaseDateText = tmdbDetail?.releaseDate?.takeIf { it.isNotBlank() }
        ?: initialReleaseDate?.takeIf { it.isNotBlank() }
    val runtimeMinutes = tmdbDetail?.runtimeMinutes
    val directors = tmdbDetail?.directors ?: emptyList()
    val castList = tmdbDetail?.cast ?: emptyList()
    val overviewText = tmdbDetail?.overview?.takeIf { it.isNotBlank() }
        ?: initialOverview?.takeIf { it.isNotBlank() }
        ?: if (isLoading) stringResource(R.string.info_loading) else stringResource(R.string.no_overview)

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.util.KeepSystemBarsHidden()
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = if (isLandscape) 820.dp else 720.dp)
                .padding(vertical = if (isLandscape) 6.dp else 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131C)),
            border = BorderStroke(1.dp, Color(0xFF282F42))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (isLandscape) 145.dp else 240.dp)
                ) {
                    androidx.compose.animation.Crossfade(
                        targetState = backdropOrPoster,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
                        modifier = Modifier.matchParentSize(),
                        label = "content_info_backdrop_slide"
                    ) { bgUrl ->
                        if (!bgUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bgUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.55f),
                                contentScale = ContentScale.Crop,
                                error = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_gallery)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF10131C).copy(alpha = 0.55f),
                                        Color(0xFF10131C).copy(alpha = 0.82f),
                                        Color(0xFF10131C)
                                    )
                                )
                            )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(if (isLandscape) 6.dp else 10.dp)
                            .size(if (isLandscape) 30.dp else 34.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_desc),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                top = if (isLandscape) 10.dp else 16.dp,
                                end = 48.dp,
                                bottom = if (isLandscape) 6.dp else 12.dp
                            )
                    ) {
                        // Top-left metadata row: IMDb Badge, Rating, Release Date, Runtime
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color(0xFF29B6F6).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(0xFF29B6F6).copy(alpha = 0.7f))
                            ) {
                                Text(
                                    text = if (isSeries) "📺 DİZİ (IMDb)" else "🎬 FİLM (IMDb)",
                                    color = Color(0xFF29B6F6),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (!ratingText.isNullOrBlank()) {
                                Surface(
                                    color = Color(0xFFF5C518),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "IMDb",
                                            color = Color.Black,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = ratingText,
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            if (!releaseDateText.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.info_release_date, releaseDateText),
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (runtimeMinutes != null && runtimeMinutes > 0) {
                                Text(
                                    text = if (isSeries) {
                                        stringResource(R.string.info_runtime_series, runtimeMinutes)
                                    } else {
                                        stringResource(R.string.info_runtime_movie, runtimeMinutes)
                                    },
                                    color = Color(0xFFFFCA28),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                        Text(
                            text = tmdbDetail?.title?.takeIf { it.isNotBlank() } ?: title,
                            color = Color.White,
                            fontSize = if (isLandscape) 17.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = if (isLandscape) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.imdb_genres_label, genres.joinToString(", ")),
                                color = Color(0xFF90CAF9),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (directors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 6.dp))
                            val joinedDirectors = directors.joinToString(", ")
                            Text(
                                text = if (isSeries) {
                                    stringResource(R.string.info_creator, joinedDirectors)
                                } else {
                                    stringResource(R.string.info_director, joinedDirectors)
                                },
                                color = Color(0xFFCFD8DC),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (castList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            val summaryNames = castList.take(5).joinToString(", ") { it.name }
                            Text(
                                text = stringResource(R.string.info_cast_summary, summaryNames),
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.5.sp,
                                maxLines = if (isLandscape) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

                        Text(
                            text = overviewText,
                            color = Color(0xFFD5DCE2),
                            fontSize = if (isLandscape) 12.sp else 13.sp,
                            lineHeight = if (isLandscape) 16.sp else 18.sp,
                            maxLines = if (isOverviewExpanded) 15 else if (isLandscape) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                        )

                        // Continue Watching + Start from Beginning buttons + thin progress bar + % (ONLY if saved resume progress exists)
                        val prog = resumeProgress
                        if (prog != null && prog.positionMs > 0L && prog.durationMs > 0L) {
                            val progressRatio = (prog.positionMs.toFloat() / prog.durationMs.toFloat()).coerceIn(0f, 1f)
                            val progressPercent = (progressRatio * 100f).toInt().coerceIn(1, 100)

                            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 14.dp))

                            if (isLandscape) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Kaldığı Yerden Devam Et
                                    Button(
                                        onClick = {
                                            com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESUME
                                            if (onPlay != null) {
                                                onPlay(matchedResumeEpisode)
                                            } else {
                                                onDismiss()
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE50914),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.resume_from_last_position),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 2. Baştan Başla
                                    OutlinedButton(
                                        onClick = {
                                            com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESTART
                                            if (onPlay != null) {
                                                onPlay(matchedResumeEpisode)
                                            } else {
                                                onDismiss()
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.65f),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.start_from_beginning),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            progress = { progressRatio },
                                            modifier = Modifier
                                                .width(110.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFFE50914),
                                            trackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "%$progressPercent",
                                            color = Color(0xFFE0E0E0),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Kaldığı Yerden Devam Et
                                        Button(
                                            onClick = {
                                                com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESUME
                                                if (onPlay != null) {
                                                    onPlay(matchedResumeEpisode)
                                                } else {
                                                    onDismiss()
                                                }
                                            },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE50914),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.resume_from_last_position),
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 2. Baştan Başla
                                        OutlinedButton(
                                            onClick = {
                                                com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESTART
                                                if (onPlay != null) {
                                                    onPlay(matchedResumeEpisode)
                                                } else {
                                                    onDismiss()
                                                }
                                            },
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Black.copy(alpha = 0.65f),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.start_from_beginning),
                                                color = Color.White,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progressRatio },
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = Color(0xFFE50914),
                                            trackColor = Color.White.copy(alpha = 0.25f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "%$progressPercent",
                                            color = Color(0xFFE0E0E0),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isLandscape) 6.dp else 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Cast section with photos
                if (castList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = if (isLandscape) 4.dp else 10.dp
                            )
                    ) {
                        Text(
                            text = stringResource(R.string.info_cast_section_title),
                            color = Color.White,
                            fontSize = if (isLandscape) 12.5.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 12.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(castList) { member ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(if (isLandscape) 68.dp else 76.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isLandscape) 48.dp else 64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF232A38)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!member.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(member.photoUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = member.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_gallery)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = member.name,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(if (isLandscape) 24.dp else 30.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = member.name,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (member.character.isNotBlank()) {
                                        Text(
                                            text = member.character,
                                            color = Color.Gray,
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImdbDetailDialog(
    item: ImdbUpcomingItem,
    isNotified: Boolean,
    onNotifyToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isSeries = item.type.equals("Dizi", ignoreCase = true) ||
        item.type.equals("TV Shows", ignoreCase = true) ||
        item.type.equals("TV Series", ignoreCase = true)

    var tmdbDetail by remember { mutableStateOf<TmdbFullDetail?>(null) }
    var isOverviewExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(item.tmdbId, item.title, isSeries) {
        tmdbDetail = fetchTmdbFullDetail(
            query = item.title,
            isSeries = isSeries,
            tmdbId = item.tmdbId
        )
    }

    val backdropList = remember(tmdbDetail, item.posterUrl) {
        val fromTmdb = tmdbDetail?.backdropUrls?.filter { it.isNotBlank() }.orEmpty()
        when {
            fromTmdb.isNotEmpty() -> fromTmdb
            !tmdbDetail?.backdropUrl.isNullOrBlank() -> listOf(tmdbDetail!!.backdropUrl!!)
            !tmdbDetail?.posterUrl.isNullOrBlank() -> listOf(tmdbDetail!!.posterUrl!!)
            item.posterUrl.isNotBlank() -> listOf(item.posterUrl)
            else -> emptyList()
        }
    }
    var currentBackdropIndex by remember(backdropList) { mutableIntStateOf(0) }
    LaunchedEffect(backdropList) {
        currentBackdropIndex = 0
        if (backdropList.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(3500L)
                currentBackdropIndex = (currentBackdropIndex + 1) % backdropList.size
            }
        }
    }
    val headerImageUrl = backdropList.getOrNull(currentBackdropIndex) ?: tmdbDetail?.backdropUrl ?: tmdbDetail?.posterUrl ?: item.posterUrl
    val runtimeMinutes = tmdbDetail?.runtimeMinutes
    val directors = tmdbDetail?.directors ?: emptyList()
    val castList = tmdbDetail?.cast ?: emptyList()
    val displayOverview = tmdbDetail?.overview?.takeIf { it.isNotBlank() } ?: item.overview

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.example.util.KeepSystemBarsHidden()
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = if (isLandscape) 780.dp else 680.dp)
                .padding(vertical = if (isLandscape) 6.dp else 16.dp, horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A28)),
            border = BorderStroke(1.dp, Color(0xFF2E2E42))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscape) 120.dp else 190.dp)
                ) {
                    androidx.compose.animation.Crossfade(
                        targetState = headerImageUrl,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 700),
                        modifier = Modifier.fillMaxSize(),
                        label = "imdb_detail_backdrop_slide"
                    ) { bgUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_gallery)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF1A1A28)),
                                    startY = if (isLandscape) 40f else 100f
                                )
                            )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(if (isLandscape) 30.dp else 36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_desc),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        color = Color(0xFFF5C518),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(if (isLandscape) 8.dp else 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = if (isLandscape) 2.dp else 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.imdbRating} IMDb",
                                color = Color.Black,
                                fontSize = if (isLandscape) 12.sp else 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isLandscape) 8.dp else 16.dp)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = if (isLandscape) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 3.dp else 6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (!isSeries) Color(0xFF1E88E5) else Color(0xFF8E24AA),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.type,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "📅 ${item.releaseDate}",
                            color = Color(0xFFFFB300),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (runtimeMinutes != null && runtimeMinutes > 0) {
                            Text(
                                text = if (isSeries) {
                                    stringResource(R.string.info_runtime_series, runtimeMinutes)
                                } else {
                                    stringResource(R.string.info_runtime_movie, runtimeMinutes)
                                },
                                color = Color(0xFFE0E0E0),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                    Text(
                        text = stringResource(R.string.imdb_genres_label, item.genres.joinToString(", ")),
                        color = Color.LightGray,
                        fontSize = 11.5.sp
                    )

                    if (directors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 6.dp))
                        val joinedDirectors = directors.joinToString(", ")
                        Text(
                            text = if (isSeries) {
                                stringResource(R.string.info_creator, joinedDirectors)
                            } else {
                                stringResource(R.string.info_director, joinedDirectors)
                            },
                            color = Color(0xFFCFD8DC),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (castList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val summaryNames = castList.take(5).joinToString(", ") { it.name }
                        Text(
                            text = stringResource(R.string.info_cast_summary, summaryNames),
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.5.sp,
                            maxLines = if (isLandscape) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 10.dp))

                    if (!isLandscape) {
                        Text(
                            text = stringResource(R.string.imdb_overview_label),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = displayOverview,
                        color = Color.Gray,
                        fontSize = if (isLandscape) 12.sp else 13.sp,
                        lineHeight = if (isLandscape) 16.sp else 18.sp,
                        maxLines = if (isOverviewExpanded) 15 else if (isLandscape) 2 else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                    )

                    if (castList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 12.dp))
                        Text(
                            text = stringResource(R.string.info_cast_section_title),
                            color = Color.White,
                            fontSize = if (isLandscape) 12.5.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 12.dp),
                            contentPadding = PaddingValues(bottom = 2.dp)
                        ) {
                            items(castList) { member ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(if (isLandscape) 64.dp else 72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isLandscape) 44.dp else 58.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF232A38)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!member.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(member.photoUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = member.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = rememberAsyncImagePainter(model = android.R.drawable.ic_menu_gallery)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = member.name,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(if (isLandscape) 22.dp else 28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = member.name,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (member.character.isNotBlank()) {
                                        Text(
                                            text = member.character,
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 12.dp))

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.LightGray
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E3E52)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.close_desc),
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = onNotifyToggle,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isNotified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isNotified) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isNotified) stringResource(R.string.imdb_notify_active) else stringResource(R.string.imdb_notify_btn),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.LightGray
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3E3E52)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.close_desc),
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onNotifyToggle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isNotified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isNotified) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isNotified) stringResource(R.string.imdb_notify_active) else stringResource(R.string.imdb_notify_btn),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
