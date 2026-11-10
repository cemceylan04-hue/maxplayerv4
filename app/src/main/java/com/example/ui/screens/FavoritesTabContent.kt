package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.db.PlaybackProgressEntity
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.theme.RedPrimary

private val CyanAccent = Color(0xFF22C7F0)
private val BorderNavy = Color(0xFF1E2838)
private val DarkCardBg = Color(0xFF0C1320)
private val TextMuted = Color(0xFF8E9BAE)

/**
 * Yeniden tasarlanan Favoriler Sekmesi (Görsel referans: image_3487d4cc.png)
 * 1. Sol Panel: Kaldığın Yerden Devam Et dikey kartları (3 adet kart, ortada Play ikonu, sağ üstte kalp, altta ilerleme)
 * 2. Sağ Üst: "SENİN FAVORİ KANALLARIN - FAVORİLER" Kırmızı Neon Slider/Hero Banner
 * 3. Sağ Orta: Filtre Çipleri (Tümü, Canlı TV'ler, Filmler, Diziler, Tümünü Gör)
 * 4. Sağ Alt: Favori içerik kartları ızgarası (Orijinal MovieCard, ChannelCard, SeriesCard)
 */
@Composable
fun FavoritesTabContent(
    recentProgressList: List<PlaybackProgressEntity>,
    allItems: List<M3uItem>,
    favLive: List<M3uItem>,
    favMovies: List<M3uItem>,
    favSeriesGroups: List<Map.Entry<String, List<M3uItem>>>,
    allSeriesMap: Map<String, List<M3uItem>>,
    isLandscape: Boolean,
    isTvDevice: Boolean = false,
    onPlayStream: (M3uItem, List<M3uItem>) -> Unit,
    onMovieClick: (M3uItem, List<M3uItem>) -> Unit,
    onSeriesClick: (String, List<M3uItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val distinctFavMovies = remember(favMovies) {
        favMovies.distinctBy { com.example.model.FavoritesManager.normalizeKey(it.title).ifBlank { it.url } }
    }
    val distinctFavLive = remember(favLive) {
        favLive.distinctBy { it.url.ifBlank { it.title } }
    }
    var selectedFilter by remember { mutableStateOf(0) } // 0: Tümü, 1: Canlı TV, 2: Film, 3: Dizi
    val totalCount = distinctFavLive.size + distinctFavMovies.size + favSeriesGroups.size

    if (isLandscape) {
        // TV / YATAY DÜZEN: Sol Kaldığın Yerden Devam Et SABİT, Sağ Slider + Filtreler + Posterler BİRLİKTE AŞAĞI KAYAR
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. SOL PANEL: KALDIĞIN YERDEN DEVAM ET (SABİT)
            FavoritesContinueWatchingSidePanel(
                recentList = recentProgressList,
                allItems = allItems,
                onPlayItem = { item ->
                    if (item.type == ItemType.MOVIE) {
                        onMovieClick(item, listOf(item))
                    } else {
                        onPlayStream(item, listOf(item))
                    }
                },
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
            )

            // 2. SAĞ ALAN: SLIDER + FİLTRELER + FAVORİ LİSTESİ BİRLİKTE AŞAĞI KAYAR
            FavoritesCatalogGrid(
                selectedFilter = selectedFilter,
                onSelectFilter = { selectedFilter = it },
                favLive = distinctFavLive,
                favMovies = distinctFavMovies,
                favSeriesGroups = favSeriesGroups,
                allSeriesMap = allSeriesMap,
                recentProgressList = emptyList(),
                allItems = allItems,
                totalCount = totalCount,
                onPlayStream = onPlayStream,
                onMovieClick = onMovieClick,
                onSeriesClick = onSeriesClick,
                isLandscape = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        // DİKEY (PORTRAIT) TELEFON DÜZENİ: TÜM SAYFA AŞAĞI KAYAR
        FavoritesCatalogGrid(
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it },
            favLive = distinctFavLive,
            favMovies = distinctFavMovies,
            favSeriesGroups = favSeriesGroups,
            allSeriesMap = allSeriesMap,
            recentProgressList = recentProgressList,
            allItems = allItems,
            totalCount = totalCount,
            onPlayStream = onPlayStream,
            onMovieClick = onMovieClick,
            onSeriesClick = onSeriesClick,
            isLandscape = false,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        )
    }
}

/**
 * Sol Panel: "KALDIĞIN YERDEN DEVAM ET" (image_3487d4cc.png)
 */
@Composable
private fun FavoritesContinueWatchingSidePanel(
    recentList: List<PlaybackProgressEntity>,
    allItems: List<M3uItem>,
    onPlayItem: (M3uItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, BorderNavy, RoundedCornerShape(14.dp)),
        color = Color(0xFF0B101A),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            // Başlık
            Text(
                text = "KALDIĞIN YERDEN\nDEVAM ET",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            HorizontalDivider(color = Color(0xFF1E2636), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 3 adet dikey kart
            val displayItems = remember(recentList, allItems) {
                if (recentList.isNotEmpty()) {
                    recentList.take(3).map { prog ->
                        val matched = allItems.find { it.url == prog.url }
                            ?: allItems.find { it.title.equals(prog.title, ignoreCase = true) }
                        val item = if (matched != null) {
                            matched.copy(url = prog.url, title = prog.title)
                        } else {
                            M3uItem(
                                title = prog.title,
                                url = prog.url,
                                logo = prog.logo,
                                type = when (prog.type.lowercase()) {
                                    "live" -> com.example.parser.ItemType.LIVE
                                    "series" -> com.example.parser.ItemType.SERIES
                                    else -> com.example.parser.ItemType.MOVIE
                                }
                            )
                        }
                        prog to item
                    }
                } else {
                    // İzleme geçmişi yoksa önerilen popüler favori içeriklerden 3 tane göster
                    allItems.take(3).map { item ->
                        PlaybackProgressEntity(
                            url = item.url,
                            title = item.title,
                            logo = item.logo,
                            type = item.type.name,
                            positionMs = 3500000,
                            durationMs = 6000000
                        ) to item
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayItems.forEach { (progress, m3uItem) ->
                    val progressPercent = if (progress.durationMs > 0L) {
                        val ratio = progress.positionMs.toFloat() / progress.durationMs.toFloat()
                        if (ratio.isNaN() || ratio.isInfinite()) 0.5f else ratio.coerceIn(0.05f, 1f)
                    } else 0.65f

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ContinueWatchingItemCard(
                            title = progress.title,
                            logo = progress.logo ?: m3uItem.logo,
                            progressPercent = progressPercent,
                            onClick = {
                                com.example.model.PlayerRepository.lastPositions[m3uItem.url] = progress.positionMs to progress.durationMs
                                com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESUME
                                onPlayItem(m3uItem)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tekil Kaldığın Yerden Devam Et Kartı (image_3487d4cc.png)
 */
@Composable
private fun ContinueWatchingItemCard(
    title: String,
    logo: String?,
    progressPercent: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) RedPrimary else Color(0xFF1A2333),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF101726)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Arka Plan Görseli
            if (!logo.isNullOrBlank()) {
                AsyncImage(
                    model = logo,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Koyu Karartma Katmanı
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF152034))
                )
            }

            // Sağ Üst Küçük Kalp İkonu ❤️
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favori",
                tint = RedPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(15.dp)
            )

            // Merkez Kırmızı Yuvarlak Oynat Butonu ▶
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(RedPrimary.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Oynat",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Alt Başlık ve Kırmızı İlerleme Çubuğu
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = RedPrimary,
                    trackColor = Color(0xFF2A364A)
                )
            }
        }
    }
}

/**
 * Sağ Üst: "FAVORİLER" Kırmızı Neon Slider/Hero Banner (image_3487d4cc.png)
 */
@Composable
private fun FavoritesHeroBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, Color(0xFF6E0D1A), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F0407)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0D0306),
                            Color(0xFF26050C),
                            Color(0xFF4C0814),
                            Color(0xFF2B040B)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol Metin Alanı
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SENİN FAVORİ KANALLARIN",
                        color = Color(0xFFE57373),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                                append("FAVORİ")
                            }
                            withStyle(style = SpanStyle(color = RedPrimary, fontWeight = FontWeight.Black)) {
                                append("LER")
                            }
                        },
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "En çok izlediğin kanallar, artık tek yerde!",
                        color = Color(0xFFD6C8CE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Sağ Taraf 3D Grafikler / Neon Kalp Görseli
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stadyum/Futbol Ekranı Kartı
                    Surface(
                        modifier = Modifier
                            .width(80.dp)
                            .height(58.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2A3A),
                        border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.6f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Kırmızı Neon Kalp İkonu
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(RedPrimary.copy(alpha = 0.5f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favoriler",
                            tint = RedPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filtre Çipleri Satırı (image_3487d4cc.png)
 * [❤️ Tümü (32)] [📺 Canlı TV'ler (11)] [🎬 Filmler (11)] [📺 Diziler (10)] --- [🔄 TÜMÜ ›]
 */
@Composable
private fun FavoritesFilterChipsRow(
    selectedFilter: Int,
    onSelectFilter: (Int) -> Unit,
    liveCount: Int,
    moviesCount: Int,
    seriesCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sol Filtre Hapları
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tümü
            FilterChipItem(
                label = "Tümü ($totalCount)",
                icon = Icons.Default.Favorite,
                isSelected = (selectedFilter == 0),
                onClick = { onSelectFilter(0) }
            )

            // Canlı TV'ler
            FilterChipItem(
                label = "Canlı TV'ler ($liveCount)",
                icon = Icons.Default.LiveTv,
                isSelected = (selectedFilter == 1),
                onClick = { onSelectFilter(1) }
            )

            // Filmler
            FilterChipItem(
                label = "Filmler ($moviesCount)",
                icon = Icons.Default.Movie,
                isSelected = (selectedFilter == 2),
                onClick = { onSelectFilter(2) }
            )

            // Diziler
            FilterChipItem(
                label = "Diziler ($seriesCount)",
                icon = Icons.Default.Tv,
                isSelected = (selectedFilter == 3),
                onClick = { onSelectFilter(3) }
            )
        }

        // Sağ "TÜMÜ ›"
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF141924),
            border = BorderStroke(1.dp, BorderNavy),
            modifier = Modifier.clickable { onSelectFilter(0) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TÜMÜ ›",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) RedPrimary else if (isFocused) Color(0xFF1E2838) else Color(0xFF101622),
        border = BorderStroke(
            width = if (isFocused) 2.dp else if (isSelected) 1.dp else 1.dp,
            color = if (isFocused || isSelected) RedPrimary else Color(0xFF1F2B3E)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else RedPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFFD0D7E4),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Sağ Alan: Slider + Filtre Çipleri + Favori Posterleri BİRLİKTE AŞAĞI KAYAR (image_3487d4cc.png)
 */
@Composable
private fun FavoritesCatalogGrid(
    selectedFilter: Int,
    onSelectFilter: (Int) -> Unit,
    favLive: List<M3uItem>,
    favMovies: List<M3uItem>,
    favSeriesGroups: List<Map.Entry<String, List<M3uItem>>>,
    allSeriesMap: Map<String, List<M3uItem>>,
    recentProgressList: List<PlaybackProgressEntity>,
    allItems: List<M3uItem>,
    totalCount: Int,
    onPlayStream: (M3uItem, List<M3uItem>) -> Unit,
    onMovieClick: (M3uItem, List<M3uItem>) -> Unit,
    onSeriesClick: (String, List<M3uItem>) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = if (isLandscape) GridCells.Fixed(5) else GridCells.Fixed(3),
        contentPadding = PaddingValues(top = 2.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. SLIDER / HERO BANNER (Tüm kolonları kaplar, liste ile birlikte yukarı kayar)
        item(span = { GridItemSpan(maxLineSpan) }) {
            FavoritesHeroBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 130.dp else 120.dp)
                    .padding(bottom = 2.dp)
            )
        }

        // 2. FİLTRE ÇİPLERİ (Tüm kolonları kaplar, liste ile birlikte kayar)
        item(span = { GridItemSpan(maxLineSpan) }) {
            FavoritesFilterChipsRow(
                selectedFilter = selectedFilter,
                onSelectFilter = onSelectFilter,
                liveCount = favLive.size,
                moviesCount = favMovies.size,
                seriesCount = favSeriesGroups.size,
                totalCount = totalCount
            )
        }

        // Portre modunda (sol panel yoksa) Kaldığın Yerden Devam Et yatay şeridi
        if (!isLandscape && recentProgressList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "KALDIĞIN YERDEN DEVAM ET",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(recentProgressList.take(5), key = { idx, prog -> "prog_${idx}_${prog.url}" }) { _, progress ->
                            val matched = allItems.find { it.url == progress.url }
                            if (matched != null) {
                                Box(modifier = Modifier.width(180.dp).height(80.dp)) {
                                    val safeProgress = if (progress.durationMs > 0L) {
                                        val ratio = progress.positionMs.toFloat() / progress.durationMs.toFloat()
                                        if (ratio.isNaN() || ratio.isInfinite()) 0.5f else ratio.coerceIn(0.05f, 1f)
                                    } else 0.5f
                                    ContinueWatchingItemCard(
                                        title = progress.title,
                                        logo = progress.logo ?: matched.logo,
                                        progressPercent = safeProgress,
                                        onClick = {
                                            if (matched.type == ItemType.MOVIE) onMovieClick(matched, listOf(matched))
                                            else onPlayStream(matched, listOf(matched))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Eğer favori içerik yoksa
        if (totalCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Henüz favori içerik eklenmedi",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 3. FAVORİ POSTER LİSTESİ (Her kart 1 kolon kaplar)
        // Canlı TV'ler (Benzersiz indexli key ile duplicate key crash'i önlenir)
        if (selectedFilter == 0 || selectedFilter == 1) {
            itemsIndexed(favLive, key = { idx, it -> "fl_${idx}_${it.url.ifBlank { it.title }}" }) { _, liveItem ->
                FavoritePosterCard(
                    item = liveItem,
                    badgeType = "CANLI",
                    isLive = true,
                    onClick = { onPlayStream(liveItem, favLive) }
                )
            }
        }

        // Filmler (Benzersiz indexli key ile duplicate key crash'i önlenir)
        if (selectedFilter == 0 || selectedFilter == 2) {
            itemsIndexed(favMovies, key = { idx, it -> "fm_${idx}_${com.example.model.FavoritesManager.normalizeKey(it.title).ifBlank { it.url }}" }) { _, movieItem ->
                FavoritePosterCard(
                    item = movieItem,
                    badgeType = "Film",
                    isLive = false,
                    onClick = { onMovieClick(movieItem, favMovies) }
                )
            }
        }

        // Diziler (Benzersiz indexli key ve firstOrNull() ile liste boşsa çökme önlenir)
        if (selectedFilter == 0 || selectedFilter == 3) {
            itemsIndexed(favSeriesGroups, key = { idx, entry -> "fs_${idx}_${entry.key}" }) { _, entry ->
                val seriesName = entry.key
                val favEpisodes = entry.value
                val primary = favEpisodes.firstOrNull()
                if (primary != null) {
                    val episodes = allSeriesMap[seriesName]?.takeIf { it.isNotEmpty() } ?: favEpisodes
                    val fallbackLogo = episodes.firstOrNull { !it.logo.isNullOrEmpty() }?.logo
                    val cardItem = if (primary.logo.isNullOrEmpty() && !fallbackLogo.isNullOrEmpty()) {
                        primary.copy(logo = fallbackLogo)
                    } else primary

                    FavoritePosterCard(
                        item = cardItem,
                        badgeType = "Dizi",
                        isLive = false,
                        episodeCount = episodes.size,
                        onClick = { onSeriesClick(seriesName, episodes) }
                    )
                }
            }
        }
    }
}

/**
 * image_3487d4cc.png'deki POSTER Kartı:
 * - 210dp dikey poster kartı
 * - Sol üstte kırmızı "❤️ FAVORİ" rozeti
 * - Sağ üstte tür rozeti ("🔴 TV", "🎬 Film", "📺 Dizi")
 * - Canlı kanallar için: Stadyum/arena arka planı üzerinde ortalanmış kanal logosu
 * - Film / Dizi için: Dikey afiş görseli (Crop) + alt/üst gölgelendirme
 * - Odaklandığında ortada kırmızı parlayan ▶ Oynat ikonu ve 3dp RedPrimary çerçeve
 * - Altta kanal/içerik adı ve HD / FHD / 4K kalite rozeti
 */
@Composable
private fun FavoritePosterCard(
    item: M3uItem,
    badgeType: String,
    isLive: Boolean,
    episodeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) RedPrimary else BorderNavy,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF091220)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLive) {
                // CANLI TV İÇİN STADYUM / ARENA POSTER ARKA PLANI (image_3487d4cc.png)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF06101E),
                                    Color(0xFF0B1B2F),
                                    Color(0xFF05111F),
                                    Color(0xFF02070D)
                                )
                            )
                        )
                )

                // Yeşil çim / stadyum ışığı efekti alt kısımda
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF0A3B22).copy(alpha = 0.45f), Color.Transparent),
                                radius = 220f
                            )
                        )
                )

                // Ortalanmış Kanal Logosu
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp)
                        .size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = item.logo,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            } else {
                // FİLM & DİZİ İÇİN DİKEY AFİŞ (POSTER)
                if (!item.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logo,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF141F32), Color(0xFF0A101C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (badgeType == "Film") Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Üst ve Alt Karartma Perdesi (Yazı ve rozetlerin net görünmesi için)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Sol Üst: Kırmızı "❤️ FAVORİ" Rozeti (image_3487d4cc.png ile birebir)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = RedPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "FAVORİ",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Sağ Üst: Tür Rozeti
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xCC050A12)
            ) {
                Text(
                    text = when (badgeType) {
                        "CANLI" -> "🔴 TV"
                        "Film" -> "🎬 Film"
                        else -> "📺 Dizi"
                    },
                    color = when (badgeType) {
                        "CANLI" -> Color(0xFFFF5252)
                        "Film" -> Color(0xFF64B5F6)
                        else -> Color(0xFFFFB300)
                    },
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            // Odaklandığında Merkezde Parlayan Kırmızı Oynat Butonu
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(RedPrimary.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Alt Kısım: Başlık ve HD / FHD / Bölüm Rozeti
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF060D17).copy(alpha = 0.92f))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )

                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xFF142032),
                    border = BorderStroke(0.5.dp, Color(0xFF283B54))
                ) {
                    val qualityText = when {
                        episodeCount > 0 -> "${episodeCount} B"
                        item.title.contains("FHD", ignoreCase = true) -> "FHD"
                        item.title.contains("4K", ignoreCase = true) || item.title.contains("UHD", ignoreCase = true) -> "4K"
                        else -> "HD"
                    }
                    Text(
                        text = qualityText,
                        color = Color(0xFFD2DEED),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
