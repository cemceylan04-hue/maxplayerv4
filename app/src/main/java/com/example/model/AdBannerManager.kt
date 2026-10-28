package com.example.model

import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdBannerItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val badge: String = "SPONSOR",
    val imageUrl: String? = null,
    val imageResId: Int? = null,
    val actionUrl: String? = null,
    val primaryColor: Long = 0xFFE50914,
    val secondaryColor: Long = 0xFF0D1B2A
)

/**
 * Manages promotional and advertising slider banners shown across
 * Canlı, Film, and Dizi tabs.
 * Contains bundled high-resolution scenic sponsor banners.
 */
object AdBannerManager {
    private val defaultAds = listOf(
        AdBannerItem(
            id = "sponsor_banner_1",
            title = "Sponsorluk Alanı",
            subtitle = "Hiç bir şekilde IPTV satıcısı reklamı alınmaz",
            badge = "SPONSORLUK",
            imageResId = R.drawable.banner_ad_1
        ),
        AdBannerItem(
            id = "sponsor_banner_2",
            title = "Doğa & Kanyon",
            subtitle = "Yüksek Kaliteli 4K Yayın Deneyimi",
            badge = "ÖNE ÇIKAN",
            imageResId = R.drawable.banner_ad_2
        ),
        AdBannerItem(
            id = "sponsor_banner_3",
            title = "Göl & Dağ Manzarası",
            subtitle = "Kesintisiz Film ve Dizi Keyfi",
            badge = "4K ULTRA HD",
            imageResId = R.drawable.banner_ad_3
        ),
        AdBannerItem(
            id = "sponsor_banner_4",
            title = "Zirve & Sis",
            subtitle = "En Yeni Vizyon İçerikleri",
            badge = "SİNEMA",
            imageResId = R.drawable.banner_ad_4
        ),
        AdBannerItem(
            id = "sponsor_banner_5",
            title = "Fiyort & Gün Batımı",
            subtitle = "Premium Canlı TV ve Eğlence",
            badge = "CANLI YAYIN",
            imageResId = R.drawable.banner_ad_5
        )
    )

    private val _isBannerEnabled = MutableStateFlow<Boolean>(true)
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
