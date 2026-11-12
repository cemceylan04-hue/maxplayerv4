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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.db.PlaybackProgressEntity
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.theme.RedPrimary

private val CyanAccent = Color(0xFF22C7F0)
private val BorderNavy = Color(0xFF1E2838)
private val TextMuted = Color(0xFF8E9BAE)

/**
 * Favoriler Sekmesi:
 * Kullanıcı isteği doğrultusunda:
 * 1. Üstte: Canlı TV Favorileri (Tek yatay şerit, 7 poster sığar, fazlası yatay kaydırılır, alt satıra geçmez)
 * 2. Altında: Film Favorileri (Tek yatay şerit, 7 poster sığar, fazlası yatay kaydırılır, alt satıra geçmez)
 * 3. Altında: Dizi Favorileri (Tek yatay şerit, 7 poster sığar, fazlası yatay kaydırılır, alt satıra geçmez)
 * Tümü film ve dizideki gibi dikey poster formatında olup kumanda D-Pad ile tam uyumludur.
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
    val totalCount = distinctFavLive.size + distinctFavMovies.size + favSeriesGroups.size

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        // Yatay (TV / Tablet) modunda tam ekranda 7 poster görünmesi için dinamik genişlik hesabı
        // 7 poster, 6 boşluk (10.dp) = 60.dp, kenar boşlukları (14.dp x 2) = 28.dp -> Toplam 88.dp
        val cardWidth: Dp = if (isLandscape) {
            ((screenWidth - 88.dp) / 7f).coerceIn(110.dp, 160.dp)
        } else {
            120.dp
        }
        val cardHeight: Dp = cardWidth * 1.50f

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ÜST HERO BANNER (Özet & Görsel Başlık)
            item {
                FavoritesHeroBanner(
                    liveCount = distinctFavLive.size,
                    moviesCount = distinctFavMovies.size,
                    seriesCount = favSeriesGroups.size,
                    totalCount = totalCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(if (isLandscape) 115.dp else 110.dp)
                )
            }

            // 2. CANLI KANALLAR SATIRI (Tek satır, 7 poster, yatay kaydırma)
            item {
                FavoriteSectionRow(
                    title = "CANLI KANALLAR",
                    icon = Icons.Default.LiveTv,
                    iconColor = Color(0xFFFF5252),
                    count = distinctFavLive.size
                ) {
                    if (distinctFavLive.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            itemsIndexed(
                                distinctFavLive,
                                key = { idx, item -> "fl_${idx}_${item.url.ifBlank { item.title }}" }
                            ) { _, liveItem ->
                                FavoritePosterCard(
                                    item = liveItem,
                                    badgeType = "CANLI",
                                    isLive = true,
                                    cardWidth = cardWidth,
                                    cardHeight = cardHeight,
                                    onClick = { onPlayStream(liveItem, distinctFavLive) }
                                )
                            }
                        }
                    } else {
                        FavoritesEmptyPlaceholderCard(
                            message = "Henüz favori canlı yayın eklenmedi. Canlı TV sekmesinden ekleyebilirsiniz.",
                            icon = Icons.Default.LiveTv,
                            cardHeight = cardHeight
                        )
                    }
                }
            }

            // 3. FİLMLER SATIRI (Canlı altında, tek satır, 7 poster, yatay kaydırma)
            item {
                FavoriteSectionRow(
                    title = "FİLMLER",
                    icon = Icons.Default.Movie,
                    iconColor = Color(0xFF42A5F5),
                    count = distinctFavMovies.size
                ) {
                    if (distinctFavMovies.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            itemsIndexed(
                                distinctFavMovies,
                                key = { idx, item -> "fm_${idx}_${com.example.model.FavoritesManager.normalizeKey(item.title).ifBlank { item.url }}" }
                            ) { _, movieItem ->
                                FavoritePosterCard(
                                    item = movieItem,
                                    badgeType = "Film",
                                    isLive = false,
                                    cardWidth = cardWidth,
                                    cardHeight = cardHeight,
                                    onClick = { onMovieClick(movieItem, distinctFavMovies) }
                                )
                            }
                        }
                    } else {
                        FavoritesEmptyPlaceholderCard(
                            message = "Henüz favori film eklenmedi. Filmler sekmesinden kalp butonuna basarak ekleyebilirsiniz.",
                            icon = Icons.Default.Movie,
                            cardHeight = cardHeight
                        )
                    }
                }
            }

            // 4. DİZİLER SATIRI (Film altında, tek satır, 7 poster, yatay kaydırma)
            item {
                FavoriteSectionRow(
                    title = "DİZİLER",
                    icon = Icons.Default.Tv,
                    iconColor = Color(0xFFFFB300),
                    count = favSeriesGroups.size
                ) {
                    if (favSeriesGroups.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            itemsIndexed(
                                favSeriesGroups,
                                key = { idx, entry -> "fs_${idx}_${entry.key}" }
                            ) { _, entry ->
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
                                        cardWidth = cardWidth,
                                        cardHeight = cardHeight,
                                        onClick = { onSeriesClick(seriesName, episodes) }
                                    )
                                }
                            }
                        }
                    } else {
                        FavoritesEmptyPlaceholderCard(
                            message = "Henüz favori dizi eklenmedi. Diziler sekmesinden kalp butonuna basarak ekleyebilirsiniz.",
                            icon = Icons.Default.Tv,
                            cardHeight = cardHeight
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bölüm Başlığı ve Yatay Kaydırma Taşıyıcısı
 */
@Composable
private fun FavoriteSectionRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    count: Int,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141E2D),
                border = BorderStroke(1.dp, Color(0xFF223248))
            ) {
                Text(
                    text = "$count",
                    color = iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

/**
 * Favori Poster Kartı:
 * Film, Dizi ve Canlı TV için birebir aynı dikey poster formatında (2:3 oran)
 * - Afiş / Logo
 * - Sol üstte kırmızı "❤️ FAVORİ" rozeti
 * - Sağ üstte tür rozeti ("🔴 TV", "🎬 Film", "📺 Dizi")
 * - Kumanda ile odaklandığında parlayan kırmızı Oynat butonu ve kalın çerçeve
 * - Altta karartılmış şerit üzerinde başlık ve kalite rozeti
 */
@Composable
private fun FavoritePosterCard(
    item: M3uItem,
    badgeType: String,
    isLive: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    episodeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) RedPrimary else BorderNavy,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF091220)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLive) {
                // CANLI TV İÇİN ARENA / STADYUM ARKA PLANI ÜZERİNDE ORTALANMIŞ LOGO
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
                        .height(70.dp)
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
                        .padding(horizontal = 10.dp)
                        .size((cardWidth * 0.60f).coerceIn(48.dp, 75.dp)),
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
                            modifier = Modifier.size(42.dp)
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
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Üst ve Alt Karartma Perdesi
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
            }

            // Sol Üst: Kırmızı "❤️ FAVORİ" Rozeti
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = RedPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "FAVORİ",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Sağ Üst: Tür Rozeti
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
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
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Odaklandığında Merkezde Parlayan Kırmızı Oynat Butonu
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RedPrimary.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Oynat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Alt Kısım: Başlık ve HD / FHD / Bölüm Rozeti
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF060D17).copy(alpha = 0.92f))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
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
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Boş Kategori Durum Kartı
 */
@Composable
private fun FavoritesEmptyPlaceholderCard(
    message: String,
    icon: ImageVector,
    cardHeight: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height((cardHeight * 0.55f).coerceIn(80.dp, 105.dp))
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0C1420),
        border = BorderStroke(1.dp, BorderNavy)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Üst Hero Banner (Özet & Görsel Başlık)
 */
@Composable
private fun FavoritesHeroBanner(
    liveCount: Int,
    moviesCount: Int,
    seriesCount: Int,
    totalCount: Int,
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
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol Metin Alanı
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SENİN FAVORİ KANALLARIN & ARŞİVİN",
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
                        fontSize = 26.sp,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Toplam $totalCount İçerik • $liveCount Canlı TV • $moviesCount Film • $seriesCount Dizi",
                        color = Color(0xFFD6C8CE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Sağ Taraf Neon Kalp Görseli
                Box(
                    modifier = Modifier
                        .size(56.dp)
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
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}
