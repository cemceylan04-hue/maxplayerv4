package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AdBannerItem
import com.example.model.AdBannerManager
import com.example.model.db.PlaybackProgressEntity
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.theme.RedPrimary

private val CyanAccent = Color(0xFF22C7F0)
private val OrangeAccent = Color(0xFFFF5722)
private val BorderNavy = Color(0xFF1E3A5F)
private val CardBg = Color(0xFF0A1322)
private val TextMuted = Color(0xFF8E9BAE)

/**
 * Film ve Dizi sekmeleri için yeni arayüz (Görsel referans: image_c0d2633a.png)
 * 1. Üst Sıra: Sol "Kaldığı Yerden Devam Et / Sonraki Bölüm", Sağ Tekli Sponsorluk Sliderı (Hızlı, Güvenli, Kesintisiz ikonlu)
 * 2. Orta Sıra: 7 Posterli Sponsorluk Şeridi
 * 3. Alt Sıra: Seçilen kategoriden gelen Film / Dizi listesi (Orijinal MovieCard / SeriesCard tasarımı ile)
 */
@Composable
fun MediaCatalogTabContent(
    isMovie: Boolean,
    currentTabItems: List<M3uItem>,
    allItems: List<M3uItem>,
    selectedGroup: String?,
    recentProgressList: List<PlaybackProgressEntity>,
    isLandscape: Boolean,
    isTvDevice: Boolean = false,
    searchQuery: String,
    isLoading: Boolean,
    onMovieClick: (M3uItem, List<M3uItem>) -> Unit,
    onSeriesClick: (String, List<M3uItem>) -> Unit,
    onPlayStream: (M3uItem, List<M3uItem>) -> Unit,
    selectedCategoryFocusRequester: FocusRequester,
    navigateToActiveCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTv = remember(context) { isTvDevice || com.example.util.isTv(context) }
    val adItems by AdBannerManager.adItems.collectAsState()

    // Kaldığı yerden devam edilecek öğeyi belirle
    val mostRecentProgress = remember(recentProgressList) {
        recentProgressList.firstOrNull()
    }

    // Eğer izleme geçmişi yoksa, kategorinin ilk öğesini öne çıkan olarak sun
    val fallbackFeaturedItem = remember(currentTabItems) {
        currentTabItems.firstOrNull()
    }

    // Filmler için kopyaları (aynı isimdeki 2-3 kopyayı) temizle
    val displayMovies = remember(currentTabItems, isMovie) {
        if (isMovie) {
            currentTabItems.distinctBy { com.example.model.FavoritesManager.normalizeKey(it.title).ifBlank { it.url } }
        } else {
            emptyList()
        }
    }

    // Dizi sekmesi için akıllı gruplandırma: Aynı isimdeki dizileri tek bir kart altında birleştirir (kopyaları önler)
    val seriesGroups = remember(currentTabItems, isMovie) {
        if (!isMovie) {
            val map = linkedMapOf<String, MutableList<M3uItem>>()
            val displayNames = mutableMapOf<String, String>()
            for (item in currentTabItems) {
                val rawName = item.seriesName?.takeIf { it.isNotBlank() } ?: item.title
                val normalizedKey = com.example.model.FavoritesManager.normalizeKey(rawName)
                if (!displayNames.containsKey(normalizedKey)) {
                    displayNames[normalizedKey] = rawName.trim().trimEnd('-', '–', '—', ':', '|', ' ')
                }
                val canonicalName = displayNames[normalizedKey] ?: rawName.trim()
                map.getOrPut(canonicalName) { mutableListOf() }.add(item)
            }
            map
        } else {
            emptyMap()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 115.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxSize()
            .focusProperties {
                left = selectedCategoryFocusRequester
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft) {
                    val moved = navigateToActiveCategory
                    moved()
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        // 1. ÜST BLOK: KALDIĞI YERDEN DEVAM ET (SOL) + TEKLİ SPONSORLUK SLIDER (SAĞ)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sol Kart: Kaldığı Yerden Devam Et / Sonraki Bölüm
                        MediaContinueWatchingCard(
                            isMovie = isMovie,
                            progress = mostRecentProgress,
                            fallbackItem = fallbackFeaturedItem,
                            allItems = allItems,
                            currentTabItems = currentTabItems,
                            onPlay = { item -> onPlayStream(item, currentTabItems) },
                            onMovieClick = { item -> onMovieClick(item, currentTabItems) },
                            modifier = Modifier
                                .weight(0.46f)
                                .fillMaxHeight()
                        )

                        // Sağ Kart: Tekli Sponsorluk Sliderı (Hızlı, Güvenli, Kesintisiz ikonları ile)
                        MediaSingleSponsorBanner(
                            adItem = adItems.firstOrNull(),
                            modifier = Modifier
                                .weight(0.54f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    // Dikey Telefon Düzeni
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaContinueWatchingCard(
                            isMovie = isMovie,
                            progress = mostRecentProgress,
                            fallbackItem = fallbackFeaturedItem,
                            allItems = allItems,
                            currentTabItems = currentTabItems,
                            onPlay = { item -> onPlayStream(item, currentTabItems) },
                            onMovieClick = { item -> onMovieClick(item, currentTabItems) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )

                        MediaSingleSponsorBanner(
                            adItem = adItems.firstOrNull(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }

        // 2. ORTA BLOK: 7 POSTERLİ SPONSORLUK ŞERİDİ
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                MediaSponsorPosterRow(ads = adItems)
            }
        }

        // 3. KATEGORİ BAŞLIĞI (Doğrudan seçilen kategori adı, Popüler etiketi yok)
        item(span = { GridItemSpan(maxLineSpan) }) {
            val categoryTitle = selectedGroup?.takeIf { it.isNotBlank() } ?: (if (isMovie) "Filmler" else "Diziler")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMovie) "🎬" else "📺",
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoryTitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentTabItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMovie) "(${displayMovies.size} Film)" else "(${seriesGroups.size} Dizi)",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. İÇERİK IZGARASI (ORİJİNAL MOVIE VE SERIES KARTLARI) VEYA DURUM
        if (isLoading && currentTabItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isMovie) "Filmler yükleniyor..." else "Diziler yükleniyor...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else if (currentTabItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Arama kriterine uygun içerik bulunamadı" else "Bu kategoride içerik bulunamadı",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else if (isMovie) {
            itemsIndexed(displayMovies, key = { idx, it -> "m_${idx}_${it.url.ifBlank { it.title }}" }) { _, movie ->
                var isAtLeftEdge by remember { mutableStateOf(false) }
                val cardModifier = Modifier
                    .onGloballyPositioned { coords ->
                        val x = coords.positionInParent().x
                        isAtLeftEdge = (x <= 150f)
                    }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft && isAtLeftEdge) {
                            navigateToActiveCategory()
                            true
                        } else {
                            false
                        }
                    }

                MovieCard(
                    item = movie,
                    modifier = cardModifier,
                    onClick = {
                        onMovieClick(movie, currentTabItems)
                    }
                )
            }
        } else {
            itemsIndexed(seriesGroups.keys.toList(), key = { idx, it -> "s_${idx}_$it" }) { _, seriesName ->
                val episodes = seriesGroups[seriesName] ?: emptyList()
                val posterItem = episodes.firstOrNull { !it.logo.isNullOrEmpty() } ?: episodes.firstOrNull()
                if (posterItem != null) {
                    var isAtLeftEdge by remember { mutableStateOf(false) }
                    val cardModifier = Modifier
                        .onGloballyPositioned { coords ->
                            val x = coords.positionInParent().x
                            isAtLeftEdge = (x <= 150f)
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft && isAtLeftEdge) {
                                navigateToActiveCategory()
                                true
                            } else {
                                false
                            }
                        }

                    SeriesCard(
                        seriesName = seriesName,
                        item = posterItem,
                        episodeCount = episodes.size,
                        episodes = episodes,
                        modifier = cardModifier,
                        onClick = {
                            onSeriesClick(seriesName, episodes)
                        },
                        onPlayEpisode = { ep ->
                            onPlayStream(ep, episodes)
                        }
                    )
                }
            }
        }
    }
}

/**
 * image_c0d2633a.png referansındaki "Sonraki Bölüm" / "Kaldığı Yerden Devam Et" Kartı
 */
@Composable
private fun MediaContinueWatchingCard(
    isMovie: Boolean,
    progress: PlaybackProgressEntity?,
    fallbackItem: M3uItem?,
    allItems: List<M3uItem>,
    currentTabItems: List<M3uItem>,
    onPlay: (M3uItem) -> Unit,
    onMovieClick: (M3uItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCardFocused by remember { mutableStateOf(false) }

    val activeItem = remember(progress, fallbackItem, allItems) {
        if (progress != null) {
            val matched = allItems.find { it.url == progress.url }
                ?: allItems.find { it.title.equals(progress.title, ignoreCase = true) }
            if (matched != null) {
                matched.copy(url = progress.url, title = progress.title)
            } else {
                M3uItem(
                    title = progress.title,
                    url = progress.url,
                    logo = progress.logo,
                    type = if (isMovie) com.example.parser.ItemType.MOVIE else com.example.parser.ItemType.SERIES
                )
            }
        } else {
            fallbackItem
        }
    }

    val displayTitle = progress?.title ?: activeItem?.title ?: if (isMovie) "Film İzle" else "Sonraki Bölüm"
    val displayPoster = progress?.logo ?: activeItem?.logo
    val progressPercent = remember(progress) {
        if (progress != null && progress.durationMs > 0L) {
            val ratio = progress.positionMs.toFloat() / progress.durationMs.toFloat()
            if (ratio.isNaN() || ratio.isInfinite()) 0.65f else ratio.coerceIn(0.05f, 1f)
        } else {
            0.65f
        }
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCardFocused) 2.5.dp else 1.dp,
                color = if (isCardFocused) OrangeAccent else BorderNavy,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                if (activeItem != null) {
                    if (progress != null) {
                        com.example.model.PlayerRepository.lastPositions[activeItem.url] = progress.positionMs to progress.durationMs
                        com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESUME
                    }
                    if (isMovie) onMovieClick(activeItem) else onPlay(activeItem)
                }
            }
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMovie) "Kaldığı Yerden Devam Et" else "Sonraki Bölüm",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Orta Alan: Afiş + Başlık + Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Afiş
                Box(
                    modifier = Modifier
                        .size(width = 62.dp, height = 76.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141F32)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!displayPoster.isNullOrBlank()) {
                        AsyncImage(
                            model = displayPoster,
                            contentDescription = displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Detaylar & Butonlar
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isMovie) "Film • İzlemeye Devam Et" else "Sezon / Bölüm • 55 dk",
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (activeItem != null) {
                                    if (progress != null) {
                                        com.example.model.PlayerRepository.lastPositions[activeItem.url] = progress.positionMs to progress.durationMs
                                        com.example.model.PlayerRepository.preselectedResumeChoice = com.example.model.ResumeChoice.RESUME
                                    }
                                    if (isMovie) onMovieClick(activeItem) else onPlay(activeItem)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "▶ Şimdi İzle",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (activeItem != null) onMovieClick(activeItem)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderNavy),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Detay",
                                color = Color(0xFFD2DCE8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Alt Kısım: İlerleme Çubuğu ve İzleme Durumu
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = progressPercent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = OrangeAccent,
                    trackColor = Color(0xFF1E2838)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (progress != null) "Kaldığınız yerden devam edin." else "Önerilen içerik.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * image_c0d2633a.png referansındaki Tekli Sponsorluk Bannerı
 * (Teknoloji Hayatınızı Kolaylaştırır, Hızlı / Güvenli / Kesintisiz ikonları ile)
 */
@Composable
private fun MediaSingleSponsorBanner(
    adItem: AdBannerItem?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) CyanAccent else BorderNavy,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                if (!adItem?.actionUrl.isNullOrBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adItem?.actionUrl))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, adItem?.title ?: "Sponsorluk", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, adItem?.title ?: "Teknoloji Sponsorluk Alanı", Toast.LENGTH_SHORT).show()
                }
            }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF091629)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Arka Plan Görseli
            if (adItem?.imageResId != null) {
                AsyncImage(
                    model = adItem.imageResId,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF071120).copy(alpha = 0.95f),
                                    Color(0xFF091629).copy(alpha = 0.70f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Metin Bloğu
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0D3866),
                        border = BorderStroke(0.5.dp, CyanAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "SPONSORLUK ALANI",
                            color = CyanAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = adItem?.title ?: "Teknoloji\nHayatınızı Kolaylaştırır",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 18.sp,
                        maxLines = 2
                    )

                    Text(
                        text = adItem?.subtitle ?: "Daha güçlü bağlantılar, daha özgür bir gelecek.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Sponsorluk Detayları", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5CAB)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Detayları İncele ›",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sağ Taraf İkon Rozetleri: Hızlı, Güvenli, Kesintisiz
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    FeaturePill(icon = Icons.Default.Wifi, label = "Hızlı")
                    FeaturePill(icon = Icons.Default.Security, label = "Güvenli")
                    FeaturePill(icon = Icons.Default.Bolt, label = "Kesintisiz")
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C223E).copy(alpha = 0.85f))
            .border(0.5.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = Color(0xFFE0EBF8),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 7 Posterli Sponsorluk Şeridi (Canlı ve Anasayfa'daki gibi)
 */
@Composable
private fun MediaSponsorPosterRow(
    ads: List<AdBannerItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val brandList = remember(ads) {
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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sponsorluk",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "Tümünü Gör ›",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(brandList) { brand ->
                var isBrandFocused by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier
                        .width(135.dp)
                        .height(78.dp)
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
                    color = Color(0xFF091322)
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
                                                Color(brand.primaryColor).copy(alpha = 0.40f),
                                                Color(0xFF070E1A)
                                            )
                                        )
                                    )
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(brand.primaryColor),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f))
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
    }
}
