package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.AdBannerItem
import com.example.model.AdBannerManager
import com.example.model.db.PlaybackProgressEntity
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.components.AdSliderBanner
import com.example.ui.theme.RedPrimary
import com.example.util.isTv

// Tasarım renk sabitleri
private val DarkNavyBg = Color(0xFF0A1B2F)
private val DarkNavyBorder = Color(0xFF1E4568)
private val CyanAccent = Color(0xFF22C7F0)
private val TextMuted = Color(0xFF8FA7C2)

/**
 * Anasayfa (Tab 0) için yenilenmiş modern arayüz bileşeni.
 * - Üst Blok: HeroBanner (%63) + Günün Maçları & Fikstür Paneli (%37)
 * - Sponsorluk Şeridi (Poster / Marka kartları: TV'de 7, Yatayda 5, Dikeyde 3 kart)
 * - Vizyondakiler (TMDb / IMDb zaman filtreli afiş satırı)
 * - İzlemeye Devam Et (Son izlenen yayınlar)
 * - Footer (Telif metni ve TV Kumanda görsel rehberi)
 */
@Composable
fun HomeTabContent(
    heroItem: M3uItem?,
    onHeroClick: (M3uItem) -> Unit,
    liveFixtures: List<MatchFixture>,
    isFixturesRefreshing: Boolean,
    onRefreshFixtures: () -> Unit,
    onShowAllFixtures: () -> Unit,
    onFixtureClick: (MatchFixture) -> Unit,
    imdbReleases: List<ImdbUpcomingItem>,
    selectedImdbTimeFrame: ImdbTimeFrame,
    onSelectImdbTimeFrame: (ImdbTimeFrame) -> Unit,
    isMediaDataRefreshing: Boolean,
    onRefreshMediaData: () -> Unit,
    notifiedTmdbIds: Set<String>,
    onToggleTmdbNotification: (ImdbUpcomingItem) -> Unit,
    onImdbClick: (ImdbUpcomingItem) -> Unit,
    recentAll: List<PlaybackProgressEntity>,
    onProgressClick: (PlaybackProgressEntity) -> Unit,
    isLandscape: Boolean,
    isPortrait: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTvDevice = remember(context) { isTv(context) }
    val ads by AdBannerManager.adItems.collectAsState()
    val isBannerEnabled by AdBannerManager.isBannerEnabled.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ÜST BLOK: SPONSOR SLIDER & GÜNÜN MAÇLARI / FİKSTÜR
        item {
            if (isLandscape) {
                // TV / Yatay Telefon Görünümü: Sol Sponsor Slider (%70) - Sağ Fikstür (%30)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val sliderWeight = if (liveFixtures.isNotEmpty()) 0.70f else 1f
                    if (isBannerEnabled && ads.isNotEmpty()) {
                        AdSliderBanner(
                            modifier = Modifier
                                .weight(sliderWeight)
                                .height(208.dp),
                            height = 208.dp,
                            onClick = { ad ->
                                if (!ad.actionUrl.isNullOrBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.actionUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "${ad.badge}: ${ad.title}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "${ad.badge}: ${ad.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else if (heroItem != null) {
                        HeroBanner(
                            item = heroItem,
                            onClick = { onHeroClick(heroItem) },
                            modifier = Modifier.weight(sliderWeight)
                        )
                    }

                    if (liveFixtures.isNotEmpty()) {
                        HomeMatchFixturesPanel(
                            fixtures = liveFixtures,
                            isRefreshing = isFixturesRefreshing,
                            onRefresh = onRefreshFixtures,
                            onShowAll = onShowAllFixtures,
                            onFixtureClick = onFixtureClick,
                            modifier = Modifier
                                .weight(0.30f)
                                .height(208.dp)
                        )
                    }
                }
            } else {
                // Dikey (Portrait) Görünüm: Alt alta tam genişlik
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isBannerEnabled && ads.isNotEmpty()) {
                        AdSliderBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            height = 140.dp,
                            onClick = { ad ->
                                if (!ad.actionUrl.isNullOrBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.actionUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "${ad.badge}: ${ad.title}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "${ad.badge}: ${ad.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else if (heroItem != null) {
                        HeroBanner(
                            item = heroItem,
                            onClick = { onHeroClick(heroItem) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (liveFixtures.isNotEmpty()) {
                        HomeMatchFixturesPanel(
                            fixtures = liveFixtures,
                            isRefreshing = isFixturesRefreshing,
                            onRefresh = onRefreshFixtures,
                            onShowAll = onShowAllFixtures,
                            onFixtureClick = onFixtureClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. SPONSORLUK ŞERİDİ (POSTER / MARKA TARZI KARTLAR)
        if (ads.isNotEmpty()) {
            item {
                HomeSponsorshipSection(
                    ads = ads,
                    isTvDevice = isTvDevice,
                    isLandscape = isLandscape,
                    screenWidthDp = configuration.screenWidthDp
                )
            }
        }

        // 3. VİZYONDAKİLER (TMDb / IMDb YAKINDA VİZYONDAKİLER)
        if (imdbReleases.isNotEmpty()) {
            item {
                HomeVizyondakilerSection(
                    imdbReleases = imdbReleases,
                    selectedTimeFrame = selectedImdbTimeFrame,
                    onSelectTimeFrame = onSelectImdbTimeFrame,
                    isRefreshing = isMediaDataRefreshing,
                    onRefresh = onRefreshMediaData,
                    notifiedTmdbIds = notifiedTmdbIds,
                    onToggleNotification = onToggleTmdbNotification,
                    onCardClick = onImdbClick
                )
            }
        }

        // 4. İZLEMEYE DEVAM ET (RECENT PROGRESS)
        if (recentAll.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.continue_watching),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentAll, key = { it.url }) { progress ->
                            ProgressCard(
                                progress = progress,
                                onClick = { onProgressClick(progress) }
                            )
                        }
                    }
                }
            }
        }

    }
}

/**
 * Günün Maçları & Fikstür Paneli (Sağ Üst Blok)
 */
@Composable
fun HomeMatchFixturesPanel(
    fixtures: List<MatchFixture>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onShowAll: () -> Unit,
    onFixtureClick: (MatchFixture) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayFixtures = remember(fixtures) { fixtures.take(5) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkNavyBorder, RoundedCornerShape(14.dp)),
        color = DarkNavyBg,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Panel Başlığı ve "TÜMÜ ›" Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.fixtures_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.6.dp,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Güncelle / Yenile Butonu
                    var isRefreshFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isRefreshFocused) CyanAccent.copy(alpha = 0.35f) else Color(0xFF132F4C),
                        border = BorderStroke(
                            width = if (isRefreshFocused) 1.5.dp else 0.5.dp,
                            color = if (isRefreshFocused) Color.White else CyanAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clickable(enabled = !isRefreshing) { onRefresh() }
                            .onFocusChanged { isRefreshFocused = it.isFocused }
                            .focusable()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Fikstürü Güncelle",
                                tint = if (isRefreshFocused) Color.White else CyanAccent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "GÜNCELLE",
                                color = if (isRefreshFocused) Color.White else CyanAccent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    var isShowAllFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isShowAllFocused) CyanAccent.copy(alpha = 0.35f) else Color(0xFF132F4C),
                        border = BorderStroke(
                            width = if (isShowAllFocused) 1.5.dp else 0.5.dp,
                            color = if (isShowAllFocused) Color.White else CyanAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clickable { onShowAll() }
                            .onFocusChanged { isShowAllFocused = it.isFocused }
                            .focusable()
                    ) {
                        Text(
                            text = "TÜMÜ ›",
                            color = if (isShowAllFocused) Color.White else CyanAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Maç Satırları (Maksimum 5 satır)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                displayFixtures.forEach { fixture ->
                    MatchFixtureRow(
                        fixture = fixture,
                        onFixtureClick = onFixtureClick
                    )
                }
            }

            // Alt Sayfalama Göstergesi (Dots indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == 0) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) CyanAccent else Color(0xFF2C4A6F))
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchFixtureRow(
    fixture: MatchFixture,
    onFixtureClick: (MatchFixture) -> Unit
) {
    var isRowFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .onFocusChanged { isRowFocused = it.isFocused }
            .clip(RoundedCornerShape(6.dp))
            .background(if (isRowFocused) Color(0xFF1E3A5F) else Color.Transparent)
            .border(
                width = if (isRowFocused) 1.5.dp else 0.dp,
                color = if (isRowFocused) CyanAccent else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onFixtureClick(fixture) }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Takım A (Ev Sahibi) Logo + İsim
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fixture.homeLogo.isNotBlank()) {
                AsyncImage(
                    model = fixture.homeLogo,
                    contentDescription = fixture.homeTeam,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = fixture.homeTeam,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Saat & Lig Bilgisi (Ortada)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = fixture.dateText.ifBlank { "VS" },
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = fixture.league.take(12),
                color = TextMuted,
                fontSize = 8.sp,
                maxLines = 1
            )
        }

        // Takım B (Deplasman) İsim + Logo
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = fixture.awayTeam,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            if (fixture.awayLogo.isNotBlank()) {
                Spacer(modifier = Modifier.width(5.dp))
                AsyncImage(
                    model = fixture.awayLogo,
                    contentDescription = fixture.awayTeam,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/**
 * Sponsorluk Şeridi (Poster / Marka Tarzı Yatay Liste)
 */
@Composable
fun HomeSponsorshipSection(
    ads: List<AdBannerItem>,
    isTvDevice: Boolean,
    isLandscape: Boolean,
    screenWidthDp: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Görünür kart sayısı: TV=7, Telefon Yatay=5, Dikey=3
    val visibleCount = if (isTvDevice) 7 else if (isLandscape) 5 else 3
    val totalSpacing = (10 * (visibleCount - 1)).dp
    val totalPadding = 32.dp
    val calculatedCardWidth = ((screenWidthDp.dp - totalPadding - totalSpacing) / visibleCount).coerceAtLeast(115.dp)

    // Mockup'taki zengin marka görsel deneyimini garanti etmek için hazır sponsorlar
    val brandDisplayList = remember(ads) {
        val brands = mutableListOf<AdBannerItem>()
        brands.addAll(ads)
        // Eğer 7'den az ise mockup referansındaki marka kartlarını yer tutucu olarak tamamla
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
        // Başlık Satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_sponsor),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Text(
                text = "Tümünü Gör ›",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Yatay Marka Kartları (LazyRow)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(brandDisplayList) { item ->
                HomeSponsorCard(
                    item = item,
                    cardWidth = calculatedCardWidth,
                    onClick = {
                        if (!item.actionUrl.isNullOrBlank()) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.actionUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "${item.badge}: ${item.title}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "${item.badge}: ${item.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Tekil Sponsor Kartı (Mockup oran ve renk şemasına uyumlu)
 */
@Composable
fun HomeSponsorCard(
    item: AdBannerItem,
    cardWidth: Dp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasImage = item.imageResId != null || !item.imageUrl.isNullOrBlank()

    Surface(
        modifier = Modifier
            .width(cardWidth)
            .height(68.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(3.dp, RedPrimary, RoundedCornerShape(10.dp))
                else Modifier.border(1.dp, DarkNavyBorder, RoundedCornerShape(10.dp))
            )
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .focusable(),
        color = if (isFocused) Color(0xFF1E3A5F) else DarkNavyBg,
        shape = RoundedCornerShape(10.dp)
    ) {
        if (hasImage) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (item.imageResId != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = item.imageResId),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Hafif karartma ve başlık
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        } else {
            // Görsel yoksa şık Logo + Başlık tasarımı
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Marka Baş Harf Rozeti
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(item.primaryColor), Color(item.secondaryColor))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.title.firstOrNull()?.uppercase() ?: "M",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            color = TextMuted,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vizyondakiler Bölümü (Zaman Filtre Çipleri ve Afiş Listesi)
 */
@Composable
fun HomeVizyondakilerSection(
    imdbReleases: List<ImdbUpcomingItem>,
    selectedTimeFrame: ImdbTimeFrame,
    onSelectTimeFrame: (ImdbTimeFrame) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    notifiedTmdbIds: Set<String>,
    onToggleNotification: (ImdbUpcomingItem) -> Unit,
    onCardClick: (ImdbUpcomingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Başlık ve Çipler Satırı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vizyondakiler",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            // Çipler: Tümü / Haftalık / 30 Günlük / 60 Günlük
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp,
                        color = CyanAccent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                ImdbTimeFrame.values().forEach { tf ->
                    val isSelected = selectedTimeFrame == tf
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanAccent else Color(0xFF101B2B),
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) CyanAccent else DarkNavyBorder
                        ),
                        modifier = Modifier.clickable { onSelectTimeFrame(tf) }
                    ) {
                        Text(
                            text = stringResource(tf.labelRes),
                            color = if (isSelected) Color(0xFF04111D) else Color(0xFFB9CADB),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Afiş Kartları (LazyRow) - Kompakt Yatay Görünüm (16:10 orantılı, ekranda aşırı yer kaplamayan modern kartlar)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(imdbReleases) { item ->
                HomeVizyondakilerCard(
                    item = item,
                    onClick = { onCardClick(item) }
                )
            }
        }
    }
}

/**
 * Vizyondakiler için Kompakt Yatay Kart Tasarımı
 * TV ekranlarında yarım ekran kaplamayan, referans görseldeki 16:10 oranlı zarif kart.
 */
@Composable
fun HomeVizyondakilerCard(
    item: ImdbUpcomingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(156.dp)
            .height(94.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.05f)
                    .border(2.5.dp, RedPrimary, RoundedCornerShape(10.dp))
                else Modifier.border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
            )
            .clickable(onClick = onClick)
            .focusable(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F1A2A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Poster / Backdrop Görseli
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Karartma Gradyanı (Metinlerin ve etiketlerin net okunması için)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.90f)
                            ),
                            startY = 15f
                        )
                    )
            )

            // Sol Üst: "Film" / Tür Rozeti
            Surface(
                color = CyanAccent,
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = item.type.ifBlank { "Film" },
                    color = Color(0xFF021020),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Alt Satır: Puan Rozeti + Film / İçerik Başlığı
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.imdbRating.isNotBlank() && item.imdbRating != "0") {
                    Surface(
                        color = Color(0xFFF5C518),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "★",
                                color = Color.Black,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.imdbRating,
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                }

                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * TV ve Yatay Mod için Ekranın Altında Sabit Kılavuz Footer Bar
 * Sarı Düğme: Favori | Yeşil Düğme: Kilit | Mavi Düğme: Bilgi
 */
@Composable
fun TvRemoteColorButtonsFooter(
    isLandscape: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        color = Color(0xF406111E), // %96 opak koyu lacivert
        border = BorderStroke(0.5.dp, Color(0xFF1E3A5F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            // Sol Taraf: Telif Hakkı ve Marka (Yatay / TV modunda)
            if (isLandscape) {
                Text(
                    text = stringResource(R.string.footer_copyright),
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Kumanda Renkli Tuş Göstergeleri (Sarı, Yeşil, Mavi)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 16.dp else 12.dp)
            ) {
                // Sarı Düğme: Favori
                RemoteColorButtonPill(
                    color = Color(0xFFFFCC00),
                    label = stringResource(R.string.footer_favorite)
                )

                // Yeşil Düğme: Kilit
                RemoteColorButtonPill(
                    color = Color(0xFF00E676),
                    label = stringResource(R.string.footer_lock)
                )

                // Mavi Düğme: Bilgi
                RemoteColorButtonPill(
                    color = Color(0xFF29B6F6),
                    label = stringResource(R.string.footer_info)
                )
            }
        }
    }
}

@Composable
private fun RemoteColorButtonPill(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Footer Bölümü (Dikey Telefon Görünümünde Listenin Sonu)
 */
@Composable
fun HomeFooterSection(
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        HorizontalDivider(color = DarkNavyBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.footer_copyright),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    RemoteColorButtonPill(color = Color(0xFFFFCC00), label = stringResource(R.string.footer_favorite))
                    RemoteColorButtonPill(color = Color(0xFF00E676), label = stringResource(R.string.footer_lock))
                    RemoteColorButtonPill(color = Color(0xFF29B6F6), label = stringResource(R.string.footer_info))
                }
            }
        }
    }
}
