package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdBannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String = "ÖNE ÇIKAN",
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val primaryColor: Long = 0xFFE50914,
    val secondaryColor: Long = 0xFF0D1B2A
)

/**
 * Manages promotional and advertising slider banners shown across
 * Anasayfa, Canlı, Film, and Dizi tabs.
 * Can be dynamically updated from a web admin panel or remote JSON endpoint.
 */
object AdBannerManager {
    private val defaultAds = listOf(
        AdBannerItem(
            id = "ad_1_vip",
            title = "MAXX PLAYER 4K ULTRA HD",
            subtitle = "Yüksek Kaliteli Kesintisiz Yayın Deneyimi ve Hızlı Akış",
            badge = "REKLAM & DUYURU",
            imageUrl = null,
            primaryColor = 0xFFE50914,
            secondaryColor = 0xFF141926
        ),
        AdBannerItem(
            id = "ad_2_sports",
            title = "CANLI SPOR & ŞAMPİYONLAR LİGİ",
            subtitle = "Süper Lig, Premier Lig ve Tüm Avrupa Maçları Canlı Yayında",
            badge = "CANLI MAÇLAR",
            imageUrl = null,
            primaryColor = 0xFF1E88E5,
            secondaryColor = 0xFF0A192F
        ),
        AdBannerItem(
            id = "ad_3_movies",
            title = "EN YENİ VİZYON FİLMLERİ VE DİZİLER",
            subtitle = "Her Gün Güncellenen Dev Sinema ve Dizi Arşivi",
            badge = "SİNEMA & DİZİ",
            imageUrl = null,
            primaryColor = 0xFF8E24AA,
            secondaryColor = 0xFF180A26
        ),
        AdBannerItem(
            id = "ad_4_panel",
            title = "WEB REKLAM YÖNETİMİ ENTEGRASYONU",
            subtitle = "Bu reklam alanını web yönetim panelinizden dinamik olarak güncelleyebilirsiniz",
            badge = "SPONSORLU ALAN",
            imageUrl = null,
            primaryColor = 0xFFFB8C00,
            secondaryColor = 0xFF211510
        )
    )

    private val _isBannerEnabled = MutableStateFlow<Boolean>(false)
    val isBannerEnabled: StateFlow<Boolean> = _isBannerEnabled.asStateFlow()

    fun setBannerEnabled(enabled: Boolean) {
        _isBannerEnabled.value = enabled
    }

    private val _adItems = MutableStateFlow<List<AdBannerItem>>(defaultAds)
    val adItems: StateFlow<List<AdBannerItem>> = _adItems.asStateFlow()

    fun updateAds(newAds: List<AdBannerItem>) {
        if (newAds.isNotEmpty()) {
            _adItems.value = newAds
        }
    }
}
