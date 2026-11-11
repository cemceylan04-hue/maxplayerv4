package com.example.ui.screens

import android.content.Context
import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlaylistRepository
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AdBannerItem
import com.example.model.AdBannerManager
import com.example.model.EpgProgram
import com.example.model.EpgRepository
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.theme.RedPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CyanAccent = Color(0xFF22C7F0)
private val DarkCardBg = Color(0xFF0C1322)
private val BorderNavy = Color(0xFF1E3A5F)
private val TextMuted = Color(0xFF8E9BAE)

/**
 * Yeniden tasarlanan Canlı TV (Tab 1) ekranı:
 * 1. Sol panel: Kategoriler listesi, tıklandığında aynı yerde kanal listesi; geri tuşunda kategorilere dönüş.
 * 2. Sağ üst: Mini TV canlı oynatıcı (OK ile tam ekran) ve yanında EPG program akış paneli.
 * 3. Sağ alt: 7 posterli sponsorluk marka şeridi ve QR Kod alanı.
 */
@Composable
fun LiveTvTabContent(
    displayGroups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    groupCounts: Map<String, Int>,
    currentTabItems: List<M3uItem>,
    isLandscape: Boolean,
    isTvDevice: Boolean = false,
    isLoading: Boolean = false,
    onPlayChannel: (M3uItem, List<M3uItem>) -> Unit,
    onRequestUnlock: (String) -> Unit = {},
    currentTabFocusRequester: FocusRequester? = null,
    categoryFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTv = remember(context) { isTvDevice || com.example.util.isTv(context) }

    // Sol panel durumu: true -> Seçilen kategorinin kanalları gösteriliyor, false -> Kategori listesi gösteriliyor
    var isViewingChannels by remember {
        mutableStateOf(!selectedGroup.isNullOrBlank() && currentTabItems.isNotEmpty())
    }

    val initialChannel = remember(currentTabItems) {
        val targetUrl = com.example.model.PlayerRepository.lastPlayedLiveChannelUrl
        val found = if (!targetUrl.isNullOrBlank()) {
            currentTabItems.find { it.url == targetUrl }
        } else null
        found ?: com.example.model.PlayerRepository.lastPlayedLiveChannel ?: currentTabItems.firstOrNull()
    }

    val initialScrollIndex = remember(initialChannel, currentTabItems) {
        val activeIdx = if (initialChannel != null) {
            currentTabItems.indexOfFirst { it.url == initialChannel.url }
        } else -1
        if (activeIdx >= 0) (activeIdx - 2).coerceAtLeast(0) else 0
    }

    val categoryListState = rememberLazyListState()
    val channelListState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex)
    val activeChannelFocusRequester = remember { FocusRequester() }
    val miniPlayerFocusRequester = remember { FocusRequester() }
    val selectedCategoryFocusRequester = categoryFocusRequester ?: remember { FocusRequester() }

    var categoryToUnlock by remember { mutableStateOf<String?>(null) }

    if (categoryToUnlock != null) {
        val grp = categoryToUnlock
        com.example.ui.components.PinUnlockDialog(
            title = grp,
            subtitle = "Kategoriye erişmek için PIN girin",
            onUnlock = {
                val targetGroup = grp
                categoryToUnlock = null
                if (targetGroup != null) {
                    com.example.model.ParentalControlManager.unlockCategoryTemporarily(targetGroup)
                    onSelectGroup(targetGroup)
                    isViewingChannels = true
                    coroutineScope.launch {
                        channelListState.scrollToItem(0)
                    }
                }
            },
            onDismiss = { categoryToUnlock = null }
        )
    }

    // O anda mini oynatıcıda ve EPG panelinde gösterilen aktif canlı kanal
    var selectedLiveChannel by remember(initialChannel) {
        mutableStateOf(initialChannel)
    }

    // currentTabItems değiştiğinde veya ekrana dönüldüğünde en son oynatılan kanalı KORU, ilk kanala ZORLA DÖNME!
    LaunchedEffect(currentTabItems, isViewingChannels) {
        if (currentTabItems.isNotEmpty()) {
            val targetUrl = com.example.model.PlayerRepository.lastPlayedLiveChannelUrl ?: selectedLiveChannel?.url
            val matching = if (!targetUrl.isNullOrBlank()) {
                currentTabItems.find { it.url == targetUrl }
            } else null

            val targetChannel = matching ?: currentTabItems.find { it.url == selectedLiveChannel?.url } ?: currentTabItems.firstOrNull()
            if (targetChannel != null) {
                selectedLiveChannel = targetChannel
                val activeIdx = currentTabItems.indexOfFirst { it.url == targetChannel.url }
                if (activeIdx >= 0 && isViewingChannels) {
                    val targetScroll = (activeIdx - 2).coerceAtLeast(0)
                    channelListState.scrollToItem(targetScroll)
                    delay(100)
                    try {
                        activeChannelFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                    delay(250)
                    try {
                        activeChannelFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Menüler arasında gezip canlıya dönüldüğünde veya kategori seçildiğinde yayınlar boşsa arka plandan hemen yükle
    LaunchedEffect(isViewingChannels, selectedGroup) {
        if (isViewingChannels && !selectedGroup.isNullOrBlank() && currentTabItems.isEmpty()) {
            PlaylistRepository.ensureCategoryLoaded(context, selectedGroup, ItemType.LIVE)
        }
    }

    // Mini ExoPlayer oynatıcısı (Preview oynatma)
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            volume = 1.0f
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Seçili kanal değiştikçe mini player stream'i hazırla ve oynat (150ms debounce ile seri geçişlerde kasma/donma yapmaz)
    LaunchedEffect(selectedLiveChannel?.url) {
        val streamUrl = selectedLiveChannel?.url
        if (!streamUrl.isNullOrBlank()) {
            delay(150)
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                android.util.Log.w("LiveTvTabContent", "Mini player stream load failed: ${e.message}")
            }
        }
    }

    // EPG verisi (Şu an ve sonraki program)
    var epgNow by remember { mutableStateOf<EpgProgram?>(null) }
    var epgNext by remember { mutableStateOf<EpgProgram?>(null) }

    LaunchedEffect(selectedLiveChannel?.url) {
        val channel = selectedLiveChannel
        if (channel != null) {
            try {
                val (now, next) = EpgRepository.getNowNext(channel)
                epgNow = now
                epgNext = next
            } catch (_: Exception) {
                epgNow = null
                epgNext = null
            }
        } else {
            epgNow = null
            epgNext = null
        }
    }

    // Kumanda GERİ tuşuna basıldığında kanal listesinden kategori listesine dön ve geçici kilitleri kapat
    BackHandler(enabled = isViewingChannels) {
        isViewingChannels = false
        com.example.model.ParentalControlManager.lockAllSessionCategories()
        coroutineScope.launch {
            val idx = displayGroups.indexOf(selectedGroup).coerceAtLeast(0)
            categoryListState.scrollToItem(idx)
            delay(120)
            try {
                selectedCategoryFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Sponsorluk verileri (7 adet marka kartı)
    val adItems by AdBannerManager.adItems.collectAsState()

    // Android TV Kumandası Renkli Tuşları: Sarı = Favori | Yeşil = Kilit | Mavi = Bilgi
    val handleRemoteColorKeys: (KeyEvent) -> Boolean = { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyUp) {
            val keyCode = keyEvent.nativeKeyEvent.keyCode
            val isYellow = keyCode == AndroidKeyEvent.KEYCODE_PROG_YELLOW ||
                keyCode == AndroidKeyEvent.KEYCODE_Y ||
                keyCode == AndroidKeyEvent.KEYCODE_F ||
                keyCode == AndroidKeyEvent.KEYCODE_MENU ||
                keyCode == AndroidKeyEvent.KEYCODE_BOOKMARK ||
                keyCode == AndroidKeyEvent.KEYCODE_WINDOW ||
                keyCode == AndroidKeyEvent.KEYCODE_STAR

            val isGreen = keyCode == AndroidKeyEvent.KEYCODE_PROG_GREEN ||
                keyCode == AndroidKeyEvent.KEYCODE_G ||
                keyCode == AndroidKeyEvent.KEYCODE_L

            val isBlue = keyCode == AndroidKeyEvent.KEYCODE_PROG_BLUE ||
                keyCode == AndroidKeyEvent.KEYCODE_INFO ||
                keyCode == AndroidKeyEvent.KEYCODE_GUIDE ||
                keyCode == AndroidKeyEvent.KEYCODE_I ||
                keyCode == AndroidKeyEvent.KEYCODE_HELP ||
                keyCode == AndroidKeyEvent.KEYCODE_B

            if (isYellow) {
                val target = selectedLiveChannel
                if (target != null) {
                    val added = com.example.model.FavoritesManager.toggleFavorite(target.url)
                    Toast.makeText(
                        context,
                        if (added) "⭐ Favorilere eklendi: ${target.title}" else "Favorilerden çıkarıldı: ${target.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                } else false
            } else if (isGreen) {
                val target = selectedLiveChannel
                if (target != null) {
                    val isLocked = com.example.model.ParentalControlManager.toggleChannelLock(target.url)
                    Toast.makeText(
                        context,
                        if (isLocked) "🔒 Kilitlendi (Varsayılan PIN: 0000): ${target.title}" else "🔓 Kilit kaldırıldı: ${target.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                } else false
            } else if (isBlue) {
                val target = selectedLiveChannel
                if (target != null) {
                    val title = epgNow?.title ?: target.title
                    val desc = epgNow?.description ?: "${target.group ?: "Canlı TV"} • Kesintisiz HD Yayın"
                    Toast.makeText(
                        context,
                        "ℹ️ $title\n$desc",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                } else false
            } else false
        } else false
    }

    if (isLandscape) {
        // TV / YATAY EKRAN DÜZENİ
        Row(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent(handleRemoteColorKeys)
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. SOL PANEL: KATEGORİ VEYA KANAL LİSTESİ (Aynı panel alanı)
            // Kategori aşamasında orijinal mavi/navy arka plan korunur; kanal listesi açıldığında kırmızı arka plan aktif olur.
            Surface(
                modifier = Modifier
                    .width(270.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = if (isViewingChannels) Color(0xFF26040A) else Color(0xFF0B101B),
                border = BorderStroke(1.dp, if (isViewingChannels) Color(0xFF6E0D1A) else BorderNavy)
            ) {
                if (!isViewingChannels) {
                    // KATEGORİLER LİSTESİ (Arka plan mavi/lacivert korunur)
                    LiveCategoryListPanel(
                        displayGroups = displayGroups,
                        selectedGroup = selectedGroup,
                        groupCounts = groupCounts,
                        listState = categoryListState,
                        onSelectCategory = { group ->
                            val isSameGroup = (group == selectedGroup)
                            com.example.model.ParentalControlManager.onCategoryChanged(group)
                            onSelectGroup(group)
                            isViewingChannels = true
                            coroutineScope.launch {
                                if (!isSameGroup) {
                                    channelListState.scrollToItem(0)
                                } else {
                                    val activeIdx = currentTabItems.indexOfFirst { it.url == selectedLiveChannel?.url }
                                    if (activeIdx >= 0) {
                                        channelListState.scrollToItem((activeIdx - 2).coerceAtLeast(0))
                                    }
                                }
                                try {
                                    activeChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                delay(50)
                                try {
                                    activeChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        },
                        onRequestUnlock = { group ->
                            categoryToUnlock = group
                        },
                        selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                        currentTabFocusRequester = currentTabFocusRequester
                    )
                } else {
                    // KANAL LİSTESİ (Arka plan kırmızı olur; ilk tıklama mini TV'de oynatır, 2. tıklama/OK tam ekran yapar)
                    LiveChannelListPanel(
                        selectedGroup = selectedGroup,
                        channels = currentTabItems,
                        selectedChannel = selectedLiveChannel,
                        listState = channelListState,
                        activeFocusRequester = activeChannelFocusRequester,
                        rightFocusRequester = miniPlayerFocusRequester,
                        onBackToCategories = {
                            isViewingChannels = false
                            com.example.model.ParentalControlManager.lockAllSessionCategories()
                            // Geri dönüldüğünde mevcut kategorinin üzerine odaklan
                            coroutineScope.launch {
                                val idx = displayGroups.indexOf(selectedGroup).coerceAtLeast(0)
                                categoryListState.scrollToItem(idx)
                                try {
                                    selectedCategoryFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                delay(50)
                                try {
                                    selectedCategoryFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        },
                        onChannelFocused = { ch ->
                            selectedLiveChannel = ch
                            com.example.model.PlayerRepository.lastPlayedLiveChannel = ch
                            com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = ch.url
                            com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = ch.group
                        },
                        onChannelClick = { ch ->
                            selectedLiveChannel = ch
                            com.example.model.PlayerRepository.lastPlayedLiveChannel = ch
                            com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = ch.url
                            com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = ch.group
                            if (selectedLiveChannel?.url == ch.url) {
                                onPlayChannel(ch, currentTabItems)
                            }
                        },
                        isLoading = isLoading
                    )
                }
            }

            // 2. SAĞ ANA ALAN: MİNİ TV OYNATICI + EPG BİLGİSİ + ALT SPONSORLUK ŞERİDİ
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ÜST KISIM: Sol Mini TV Oynatıcı (%68) - Sağ EPG Paneli (%32)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mini TV Oynatıcı (Kumandadan Sol Tuşu ile Kanal Listesindeki Aktif Kanala Döner)
                    LiveMiniPlayerCard(
                        channel = selectedLiveChannel,
                        exoPlayer = exoPlayer,
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight()
                            .focusRequester(miniPlayerFocusRequester)
                            .focusProperties {
                                left = activeChannelFocusRequester
                            },
                        onFullScreen = {
                            selectedLiveChannel?.let { ch ->
                                com.example.model.PlayerRepository.lastPlayedLiveChannel = ch
                                com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = ch.url
                                com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = ch.group
                                onPlayChannel(ch, currentTabItems)
                            }
                        }
                    )

                    // EPG Bilgi Paneli (Kumandadan Sol Tuşu ile Kanal Listesindeki Aktif Kanala Döner)
                    LiveEpgInfoPanel(
                        channel = selectedLiveChannel,
                        epgNow = epgNow,
                        epgNext = epgNext,
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight()
                            .focusProperties {
                                left = activeChannelFocusRequester
                            }
                    )
                }

                // ALT KISIM: 7 POSTERLİ SPONSORLUK ŞERİDİ & QR KODU (Sol Tuşu ile Aktif Kanala Döner)
                LiveSponsorSection(
                    ads = adItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(118.dp)
                        .focusProperties {
                            left = activeChannelFocusRequester
                        }
                )
            }
        }
    } else {
        // DİKEY (PORTRAIT) DÜZEN
        Column(
            modifier = modifier
                .fillMaxSize()
                .onPreviewKeyEvent(handleRemoteColorKeys)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Üstte Mini Player (16:9)
            LiveMiniPlayerCard(
                channel = selectedLiveChannel,
                exoPlayer = exoPlayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                onFullScreen = {
                    selectedLiveChannel?.let { ch ->
                        com.example.model.PlayerRepository.lastPlayedLiveChannel = ch
                        com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = ch.url
                        com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = selectedGroup
                        onPlayChannel(ch, currentTabItems)
                    }
                }
            )

            // EPG Bilgisi
            LiveEpgInfoPanel(
                channel = selectedLiveChannel,
                epgNow = epgNow,
                epgNext = epgNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            // Kategoriler / Kanallar Sekmesi
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = if (isViewingChannels) Color(0xFF26040A) else Color(0xFF0B101B),
                border = BorderStroke(1.dp, if (isViewingChannels) Color(0xFF6E0D1A) else BorderNavy)
            ) {
                if (!isViewingChannels) {
                    LiveCategoryListPanel(
                        displayGroups = displayGroups,
                        selectedGroup = selectedGroup,
                        groupCounts = groupCounts,
                        listState = categoryListState,
                        onSelectCategory = { group ->
                            val isSameGroup = (group == selectedGroup)
                            com.example.model.ParentalControlManager.onCategoryChanged(group)
                            onSelectGroup(group)
                            isViewingChannels = true
                            coroutineScope.launch {
                                if (!isSameGroup) {
                                    channelListState.scrollToItem(0)
                                } else {
                                    val activeIdx = currentTabItems.indexOfFirst { it.url == selectedLiveChannel?.url }
                                    if (activeIdx >= 0) {
                                        channelListState.scrollToItem((activeIdx - 2).coerceAtLeast(0))
                                    }
                                }
                                delay(100)
                                try {
                                    activeChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                                delay(250)
                                try {
                                    activeChannelFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        },
                        onRequestUnlock = { group ->
                            categoryToUnlock = group
                        },
                        selectedCategoryFocusRequester = selectedCategoryFocusRequester,
                        currentTabFocusRequester = currentTabFocusRequester
                    )
                } else {
                    LiveChannelListPanel(
                        selectedGroup = selectedGroup,
                        channels = currentTabItems,
                        selectedChannel = selectedLiveChannel,
                        listState = channelListState,
                        activeFocusRequester = activeChannelFocusRequester,
                        rightFocusRequester = miniPlayerFocusRequester,
                        onBackToCategories = {
                            isViewingChannels = false
                            com.example.model.ParentalControlManager.lockAllSessionCategories()
                            coroutineScope.launch {
                                val idx = displayGroups.indexOf(selectedGroup).coerceAtLeast(0)
                                categoryListState.scrollToItem(idx)
                                delay(120)
                                try {
                                    selectedCategoryFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            }
                        },
                        onChannelFocused = {
                            selectedLiveChannel = it
                            com.example.model.PlayerRepository.lastPlayedLiveChannel = it
                            com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = it.url
                            com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = selectedGroup
                        },
                        onChannelClick = { ch ->
                            com.example.model.PlayerRepository.lastPlayedLiveChannel = ch
                            com.example.model.PlayerRepository.lastPlayedLiveChannelUrl = ch.url
                            com.example.model.PlayerRepository.lastPlayedLiveChannelGroup = selectedGroup
                            if (selectedLiveChannel?.url == ch.url) {
                                onPlayChannel(ch, currentTabItems)
                            } else {
                                selectedLiveChannel = ch
                            }
                        },
                        isLoading = isLoading
                    )
                }
            }

            // Alt Sponsorluk
            LiveSponsorSection(
                ads = adItems,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
        }
    }
}

/**
 * Sol Panel: Kategori Listesi
 */
@Composable
private fun LiveCategoryListPanel(
    displayGroups: List<String>,
    selectedGroup: String?,
    groupCounts: Map<String, Int>,
    listState: LazyListState,
    onSelectCategory: (String) -> Unit,
    onRequestUnlock: (String) -> Unit,
    selectedCategoryFocusRequester: FocusRequester? = null,
    currentTabFocusRequester: FocusRequester? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Başlık
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KATEGORİLER",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RedPrimary.copy(alpha = 0.2f),
                border = BorderStroke(0.5.dp, RedPrimary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "${displayGroups.size}",
                    color = RedPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF1E2838), thickness = 1.dp)

        val context = LocalContext.current
        val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
        val lockedGroups by com.example.model.ParentalControlManager.lockedGroups.collectAsState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(displayGroups, key = { index, g -> "${index}_$g" }) { index, group ->
                val isSelected = (group == selectedGroup)
                val hasSelectedCategory = displayGroups.contains(selectedGroup)
                val isFocusTarget = if (hasSelectedCategory) isSelected else (index == 0)
                var isItemFocused by remember { mutableStateOf(false) }
                val isLocked = com.example.model.ParentalControlManager.isGroupLocked(group)
                val count = groupCounts[group] ?: 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isItemFocused -> RedPrimary.copy(alpha = 0.35f)
                                isSelected -> Color(0xFF16253B)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = if (isItemFocused) 2.5.dp else if (isSelected) 1.dp else 0.dp,
                            color = if (isItemFocused) RedPrimary else if (isSelected) BorderNavy else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (isFocusTarget && selectedCategoryFocusRequester != null) {
                                Modifier.focusRequester(selectedCategoryFocusRequester)
                            } else Modifier
                        )
                        .focusProperties {
                            if (index == 0 && currentTabFocusRequester != null) {
                                up = currentTabFocusRequester
                            }
                        }
                        .onFocusChanged { isItemFocused = it.isFocused }
                        .tvRemoteCardClickable(
                            onClick = {
                                if (isLocked) {
                                    onRequestUnlock(group)
                                } else {
                                    onSelectCategory(group)
                                }
                            },
                            onLongClick = {
                                Toast.makeText(
                                    context,
                                    "Kanal listesine geçerek kanalları Sarı tuşla favoriye ekleyebilirsiniz",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onLockToggle = {
                                com.example.model.ParentalControlManager.toggleGroupLock(group)
                                val isNowLocked = com.example.model.ParentalControlManager.isGroupLocked(group)
                                Toast.makeText(
                                    context,
                                    if (isNowLocked) "🔒 Kategori kilitlendi (Varsayılan PIN: 0000): $group" else "🔓 Kategori kilidi kaldırıldı: $group",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onInfoClick = {
                                Toast.makeText(
                                    context,
                                    "ℹ️ Kategori: $group ($count kanal)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = if (isItemFocused || isSelected) RedPrimary else TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = group,
                            color = if (isItemFocused || isSelected) Color.White else Color(0xFFD0D7E2),
                            fontSize = 13.sp,
                            fontWeight = if (isItemFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (count > 0) {
                        Text(
                            text = "$count",
                            color = if (isItemFocused) Color.White else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sol Panel: Seçilen Kategorinin Kanal Listesi (Görsel referans: image_f6d87f77.png)
 */
@Composable
private fun LiveChannelListPanel(
    selectedGroup: String?,
    channels: List<M3uItem>,
    selectedChannel: M3uItem?,
    listState: LazyListState,
    onBackToCategories: () -> Unit,
    onChannelFocused: (M3uItem) -> Unit,
    onChannelClick: (M3uItem) -> Unit,
    activeFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    isLoading: Boolean = false
) {
    // Seçili/oynatılan kanal liste dışındaysa görünür alana getir ve odaklan
    LaunchedEffect(selectedGroup, channels.isNotEmpty()) {
        if (channels.isNotEmpty()) {
            val activeIndex = channels.indexOfFirst { it.url == selectedChannel?.url }
            val targetIdx = if (activeIndex >= 0) activeIndex else 0
            val targetScroll = (targetIdx - 2).coerceAtLeast(0)
            listState.scrollToItem(targetScroll)
            delay(100)
            try {
                activeFocusRequester?.requestFocus()
            } catch (_: Exception) {}
            delay(250)
            try {
                activeFocusRequester?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val categoriesButtonFocusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Geri Butonu: ‹ KATEGORİLER
        var isBackFocused by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isBackFocused) RedPrimary.copy(alpha = 0.45f) else Color(0xFF38060E))
                .border(
                    width = if (isBackFocused) 2.dp else 1.dp,
                    color = if (isBackFocused) RedPrimary else Color(0xFF6E0D1A),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onBackToCategories() }
                .onFocusChanged { isBackFocused = it.isFocused }
                .focusRequester(categoriesButtonFocusRequester)
                .focusProperties {
                    up = categoriesButtonFocusRequester
                    if (activeFocusRequester != null) {
                        down = activeFocusRequester
                    }
                    if (rightFocusRequester != null) {
                        right = rightFocusRequester
                    }
                }
                .focusable()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = RedPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "‹ KATEGORİLER",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = selectedGroup ?: "Tüm Kanallar",
                    color = Color(0xFFFF8595),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(color = Color(0xFF4A0814), thickness = 1.dp)

        val context = LocalContext.current
        if (channels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = RedPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kanallar yükleniyor...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "Bu kategoride kanal bulunamadı",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val sessionUnlockedGroups by com.example.model.ParentalControlManager.sessionUnlockedGroups.collectAsState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                itemsIndexed(channels, key = { index, it -> "ch_${index}_${it.url.ifBlank { it.title }}" }) { channelIndex, channel ->
                    val isCurrent = (channel.url == selectedChannel?.url)
                    var isFocused by remember { mutableStateOf(false) }
                    val favoriteUrls by com.example.model.FavoritesManager.favoriteUrls.collectAsState()
                    val isFav = favoriteUrls.contains(channel.url)
                    val isLocked = com.example.model.ParentalControlManager.isItemLocked(channel)
                    val hasSelectedInList = channels.any { it.url == selectedChannel?.url }
                    val isTargetFocusItem = if (hasSelectedInList) {
                        channel.url == selectedChannel?.url
                    } else {
                        channelIndex == 0
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isFocused -> RedPrimary.copy(alpha = 0.45f)
                                    isCurrent -> Color(0xFF4A0814)
                                    else -> Color(0xFF1E0409)
                                }
                            )
                            .border(
                                width = if (isFocused) 3.dp else if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isFocused) RedPrimary else if (isCurrent) Color(0xFF8E0F23) else Color(0xFF38060E),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .then(
                                if (isTargetFocusItem && activeFocusRequester != null) {
                                    Modifier.focusRequester(activeFocusRequester)
                                } else Modifier
                            )
                            .focusProperties {
                                if (channelIndex == 0) {
                                    up = categoriesButtonFocusRequester
                                }
                                if (rightFocusRequester != null) {
                                    right = rightFocusRequester
                                }
                            }
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) {
                                    onChannelFocused(channel)
                                }
                            }
                            .tvRemoteCardClickable(
                                onClick = { onChannelClick(channel) },
                                onLongClick = {
                                    val added = com.example.model.FavoritesManager.toggleFavorite(channel.url)
                                    Toast.makeText(
                                        context,
                                        if (added) "⭐ Favorilere eklendi: ${channel.title}" else "Favorilerden çıkarıldı: ${channel.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onLockToggle = {
                                    val locked = com.example.model.ParentalControlManager.toggleChannelLock(channel.url)
                                    Toast.makeText(
                                        context,
                                        if (locked) "🔒 Kilitlendi (Varsayılan PIN: 0000): ${channel.title}" else "🔓 Kilit kaldırıldı: ${channel.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onInfoClick = {
                                    onChannelFocused(channel)
                                    Toast.makeText(
                                        context,
                                        "ℹ️ ${channel.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kanal Logosu ve CANLI rozeti
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2E060D)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel.logo.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel.logo,
                                    contentDescription = channel.title,
                                    modifier = Modifier.fillMaxSize().padding(3.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = RedPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Kanal Adı ve Canlı Durumu
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = channel.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(RedPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CANLI",
                                    color = RedPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Sağ Taraf: Favori ve Kilit İkonları
                        if (isFav) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favori",
                                tint = RedPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        if (isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Kilitli",
                                tint = Color(0xFFFFA726),
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sağ Üst: Mini TV Oynatıcı Kartı (image_f6d87f77.png'deki görünüm)
 */
@Composable
private fun LiveMiniPlayerCard(
    channel: M3uItem?,
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onFullScreen: () -> Unit
) {
    val context = LocalContext.current
    var isPlayerFocused by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isPlayerFocused) 3.dp else 1.5.dp,
                color = if (isPlayerFocused) RedPrimary else BorderNavy,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isPlayerFocused = it.isFocused }
            .tvRemoteCardClickable(
                onClick = { onFullScreen() },
                onLongClick = {
                    channel?.let { ch ->
                        val added = com.example.model.FavoritesManager.toggleFavorite(ch.url)
                        Toast.makeText(
                            context,
                            if (added) "⭐ Favorilere eklendi: ${ch.title}" else "Favorilerden çıkarıldı: ${ch.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onLockToggle = {
                    channel?.let { ch ->
                        val locked = com.example.model.ParentalControlManager.toggleChannelLock(ch.url)
                        Toast.makeText(
                            context,
                            if (locked) "🔒 Kilitlendi (Varsayılan PIN: 0000): ${ch.title}" else "🔓 Kilit kaldırıldı: ${ch.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onInfoClick = {
                    channel?.let { ch ->
                        Toast.makeText(
                            context,
                            "ℹ️ ${ch.title} - Canlı TV Yayını",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ),
        color = Color(0xFF060A12),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Görüntüsü (PlayerView)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { pv ->
                    pv.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )

            // Üst Sol Kanal Rozeti (Örn: TRT 1 CANLI)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color(0xFF334A6E), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channel?.title?.take(16) ?: "CANLI TV",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(RedPrimary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "CANLI",
                    color = RedPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Üst Sağ HD Rozeti
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = BorderStroke(0.5.dp, Color.Gray)
            ) {
                Text(
                    text = "HD",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Alt Kontrol & İlerleme Çubuğu (Görsel tasarımdaki kırmızı bar ve simgeler)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Oynat/Durdur",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = channel?.title ?: "TRT 1 HD",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Kırmızı İlerleme Çubuğu (Canlı yayın göstergesi)
                    LinearProgressIndicator(
                        progress = 0.72f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = RedPrimary,
                        trackColor = Color(0xFF2C394E)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Ses",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = { onFullScreen() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Tam Ekran",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sağ Üst: EPG Program Akış Paneli (image_f6d87f77.png'deki görünüm)
 */
@Composable
private fun LiveEpgInfoPanel(
    channel: M3uItem?,
    epgNow: EpgProgram?,
    epgNext: EpgProgram?,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val nowTimeStr = remember(epgNow) {
        if (epgNow != null && epgNow.startMs > 0 && epgNow.endMs > 0) {
            "${timeFormat.format(java.util.Date(epgNow.startMs))} - ${timeFormat.format(java.util.Date(epgNow.endMs))}"
        } else {
            null
        }
    }
    val nextTimeStr = remember(epgNext) {
        if (epgNext != null && epgNext.startMs > 0) {
            timeFormat.format(java.util.Date(epgNext.startMs))
        } else {
            null
        }
    }
    val nowMs = System.currentTimeMillis()
    val progressPercent = remember(epgNow, nowMs) {
        if (epgNow != null && epgNow.endMs > epgNow.startMs) {
            ((nowMs - epgNow.startMs).toFloat() / (epgNow.endMs - epgNow.startMs).toFloat()).coerceIn(0.05f, 0.98f)
        } else {
            0.50f
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF091220),
        border = BorderStroke(1.5.dp, BorderNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık: Kanal Adı ve CANLI Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel?.title ?: "Canlı TV",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(RedPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CANLI",
                            color = RedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (!channel?.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel?.logo,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E2F46), thickness = 1.dp)

            // Şu an yayınlanıyor bilgisi (İzlenen kanaldaki gerçek film/program bilgisi)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (nowTimeStr != null) "Şu an yayınlanıyor ($nowTimeStr)" else "Şu an yayınlanıyor",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = epgNow?.title ?: channel?.title ?: "Canlı Yayın Akışı",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = epgNow?.description?.takeIf { it.isNotBlank() } ?: "${channel?.group ?: "Canlı TV"} • Kesintisiz HD Yayın",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = RedPrimary,
                    trackColor = Color(0xFF1E2838)
                )
            }

            // Sonraki Yayın
            Column {
                Text(
                    text = "SONRAKİ YAYIN",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (epgNext != null) {
                        if (nextTimeStr != null) "$nextTimeStr • ${epgNext.title}" else epgNext.title
                    } else {
                        "Yayın akışı devam ediyor"
                    },
                    color = Color(0xFFD6DFEC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF122236),
                        border = BorderStroke(0.5.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${channel?.group?.take(16) ?: "Canlı"} ›",
                            color = CyanAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sağ Alt: 7 Posterli Sponsorluk Şeridi & QR Kod Alanı (image_f6d87f77.png & İstem maddesi 3)
 */
@Composable
private fun LiveSponsorSection(
    ads: List<AdBannerItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Mockup referansındaki zengin 7 marka listesini hazırla
    val sponsorBrandList = remember(ads) {
        val brands = mutableListOf<AdBannerItem>()
        brands.addAll(ads)
        val mockBrands = listOf(
            AdBannerItem("mb_1", "LC WAIKIKI", "iyi giyim, iyi hisset", "SPONSOR", primaryColor = 0xFF005BAC),
            AdBannerItem("mb_2", "Marka 1", "Teknoloji & İnovasyon", "SPONSOR", primaryColor = 0xFF00A3FF),
            AdBannerItem("mb_3", "Marka 2", "Spor & Yaşam", "SPONSOR", primaryColor = 0xFFE50914),
            AdBannerItem("mb_4", "Marka 3", "Dijital Dünya", "SPONSOR", primaryColor = 0xFF00D2D3),
            AdBannerItem("mb_5", "Marka 4", "Finans & Gelecek", "SPONSOR", primaryColor = 0xFFFF9F1A),
            AdBannerItem("mb_6", "Marka 5", "Yaratıcı Medya", "SPONSOR", primaryColor = 0xFF8E44AD),
            AdBannerItem("mb_7", "Marka 6", "Doğa & Enerji", "SPONSOR", primaryColor = 0xFF27AE60)
        )
        for (mb in mockBrands) {
            if (brands.size < 7) {
                brands.add(mb)
            }
        }
        brands
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sol Kısım: 7 Posterli Yatay Sponsorluk Listesi
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sponsorBrandList) { brand ->
                var isBrandFocused by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .width(138.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isBrandFocused) 2.5.dp else 1.dp,
                            color = if (isBrandFocused) RedPrimary else BorderNavy,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            Toast.makeText(context, "${brand.title} Sponsorluk", Toast.LENGTH_SHORT).show()
                        }
                        .onFocusChanged { isBrandFocused = it.isFocused }
                        .focusable(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF091220)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (brand.imageResId != null) {
                            AsyncImage(
                                model = brand.imageResId,
                                contentDescription = brand.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(brand.primaryColor).copy(alpha = 0.45f),
                                                Color(0xFF070D18)
                                            )
                                        )
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(brand.primaryColor),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = brand.badge,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = brand.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = brand.subtitle,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sağ Kısım: Görseldeki Neon Çerçeveli QR Kod Alanı (image_f6d87f77.png)
        Surface(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF091322),
            border = BorderStroke(2.dp, CyanAccent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "QR Kod",
                    tint = CyanAccent,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "KAMPANYAYI TARA",
                    color = CyanAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
