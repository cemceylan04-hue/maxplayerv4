package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.AdBannerItem
import com.example.model.AdBannerManager
import kotlinx.coroutines.delay

@Composable
fun AdSliderBanner(
    modifier: Modifier = Modifier,
    height: Dp = 108.dp,
    onClick: ((AdBannerItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val ads by AdBannerManager.adItems.collectAsState()

    if (ads.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFocused by remember { mutableStateOf(false) }

    // Auto slide every 4.5 seconds
    LaunchedEffect(ads.size) {
        if (ads.size > 1) {
            while (true) {
                delay(4500L)
                currentIndex = (currentIndex + 1) % ads.size
            }
        }
    }

    val currentAd = ads.getOrNull(currentIndex) ?: ads.first()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier
                    .scale(1.02f)
                    .border(3.dp, Color(0xFFE50914), RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, Color(0xFF2A3142), RoundedCornerShape(12.dp))
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (onClick != null) {
                    onClick(currentAd)
                } else {
                    Toast.makeText(context, "${currentAd.badge}: ${currentAd.title}", Toast.LENGTH_SHORT).show()
                }
            }
            .focusable()
    ) {
        Crossfade(
            targetState = currentAd,
            animationSpec = tween(600),
            label = "ad_slider_crossfade"
        ) { ad ->
            val primaryColor = Color(ad.primaryColor)
            val secondaryColor = Color(ad.secondaryColor)

            Box(modifier = Modifier.fillMaxSize()) {
                if (!ad.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(ad.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = ad.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Darkening gradient over image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0D111A).copy(alpha = 0.95f),
                                        Color(0xFF0D111A).copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else {
                    // Deep gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        secondaryColor,
                                        primaryColor.copy(alpha = 0.5f),
                                        Color(0xFF0C0F17)
                                    )
                                )
                            )
                    )

                    // Watermark app icon / graphic on the right
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(height * 0.95f)
                                .scale(1.1f),
                            alpha = 0.18f,
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Foreground Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Text Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Badge Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = primaryColor.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (ad.badge.contains("CANLI")) Icons.Default.PlayArrow else Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ad.badge.uppercase(),
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Title
                        Text(
                            text = ad.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Subtitle
                        Text(
                            text = ad.subtitle,
                            color = Color(0xFFD0D7DE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Side: 4K Ultra HD Badge + Action Arrow
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color(0xFF3E485E))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "4K PRO",
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Slide Dots Indicator at bottom right
                if (ads.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ads.forEachIndexed { idx, _ ->
                            val isActive = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .size(width = if (isActive) 16.dp else 6.dp, height = 5.dp)
                                    .background(
                                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
