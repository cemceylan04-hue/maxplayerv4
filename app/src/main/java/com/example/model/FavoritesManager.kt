package com.example.model

import android.content.Context
import android.content.SharedPreferences
import com.example.parser.ItemType
import com.example.parser.M3uItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITE_URLS = "favorite_urls"
    private const val KEY_FAVORITE_SERIES = "favorite_series_names"
    private const val KEY_FAVORITE_MOVIES = "favorite_movie_titles"

    const val FAVORITES_CATEGORY_NAME = "⭐ Favoriler"

    private lateinit var prefs: SharedPreferences

    private val _favoriteUrls = MutableStateFlow<Set<String>>(emptySet())
    val favoriteUrls: StateFlow<Set<String>> = _favoriteUrls.asStateFlow()

    private val _favoriteSeriesNames = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSeriesNames: StateFlow<Set<String>> = _favoriteSeriesNames.asStateFlow()

    private val _favoriteMovieTitles = MutableStateFlow<Set<String>>(emptySet())
    val favoriteMovieTitles: StateFlow<Set<String>> = _favoriteMovieTitles.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _favoriteUrls.value = prefs.getStringSet(KEY_FAVORITE_URLS, emptySet()) ?: emptySet()
        _favoriteSeriesNames.value = prefs.getStringSet(KEY_FAVORITE_SERIES, emptySet()) ?: emptySet()
        _favoriteMovieTitles.value = prefs.getStringSet(KEY_FAVORITE_MOVIES, emptySet()) ?: emptySet()
    }

    fun normalizeKey(name: String): String {
        return name.trim().trimEnd('-', '–', '—', ':', '|', ' ').trim().lowercase()
    }

    fun toggleFavorite(url: String): Boolean {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return false
        val current = _favoriteUrls.value.toMutableSet()
        val willBeFav = !current.contains(cleanUrl)
        if (willBeFav) {
            current.add(cleanUrl)
        } else {
            current.remove(cleanUrl)
        }
        _favoriteUrls.value = current
        prefs.edit().putStringSet(KEY_FAVORITE_URLS, current).apply()
        return willBeFav
    }

    fun isFavorite(url: String): Boolean {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return false
        return _favoriteUrls.value.contains(cleanUrl)
    }

    fun isMovieFavorite(movieItem: M3uItem): Boolean {
        if (isFavorite(movieItem.url)) return true
        val normTitle = normalizeKey(movieItem.title)
        if (normTitle.isNotBlank() && _favoriteMovieTitles.value.contains(normTitle)) return true
        return false
    }

    fun toggleMovieFavorite(movieItem: M3uItem): Boolean {
        val currentlyFav = isMovieFavorite(movieItem)
        val cleanUrl = movieItem.url.trim()
        val normTitle = normalizeKey(movieItem.title)

        val curUrls = _favoriteUrls.value.toMutableSet()
        val curMovies = _favoriteMovieTitles.value.toMutableSet()

        if (currentlyFav) {
            if (cleanUrl.isNotBlank()) curUrls.remove(cleanUrl)
            if (normTitle.isNotBlank()) curMovies.remove(normTitle)
            _favoriteUrls.value = curUrls
            _favoriteMovieTitles.value = curMovies
            prefs.edit()
                .putStringSet(KEY_FAVORITE_URLS, curUrls)
                .putStringSet(KEY_FAVORITE_MOVIES, curMovies)
                .apply()
            return false
        } else {
            if (cleanUrl.isNotBlank()) curUrls.add(cleanUrl)
            if (normTitle.isNotBlank()) curMovies.add(normTitle)
            _favoriteUrls.value = curUrls
            _favoriteMovieTitles.value = curMovies
            prefs.edit()
                .putStringSet(KEY_FAVORITE_URLS, curUrls)
                .putStringSet(KEY_FAVORITE_MOVIES, curMovies)
                .apply()
            return true
        }
    }

    fun isSeriesFavorite(seriesItem: M3uItem, episodes: List<M3uItem> = emptyList()): Boolean {
        if (isFavorite(seriesItem.url)) return true
        val normName = normalizeKey(seriesItem.seriesName ?: seriesItem.title)
        if (normName.isNotBlank() && _favoriteSeriesNames.value.contains(normName)) return true
        return episodes.any { ep ->
            isFavorite(ep.url) || (ep.seriesName?.let { normalizeKey(it) }?.let { _favoriteSeriesNames.value.contains(it) } == true)
        }
    }

    fun toggleSeriesFavorite(seriesItem: M3uItem, episodes: List<M3uItem> = emptyList()): Boolean {
        val currentlyFav = isSeriesFavorite(seriesItem, episodes)
        val cleanUrl = seriesItem.url.trim()
        val normName = normalizeKey(seriesItem.seriesName ?: seriesItem.title)

        val curUrls = _favoriteUrls.value.toMutableSet()
        val curSeries = _favoriteSeriesNames.value.toMutableSet()

        if (currentlyFav) {
            if (cleanUrl.isNotBlank()) curUrls.remove(cleanUrl)
            if (normName.isNotBlank()) curSeries.remove(normName)
            episodes.forEach { ep ->
                val epClean = ep.url.trim()
                if (epClean.isNotBlank()) curUrls.remove(epClean)
            }
            _favoriteUrls.value = curUrls
            _favoriteSeriesNames.value = curSeries
            prefs.edit()
                .putStringSet(KEY_FAVORITE_URLS, curUrls)
                .putStringSet(KEY_FAVORITE_SERIES, curSeries)
                .apply()
            return false
        } else {
            if (cleanUrl.isNotBlank()) curUrls.add(cleanUrl)
            if (normName.isNotBlank()) curSeries.add(normName)
            episodes.forEach { ep ->
                val epClean = ep.url.trim()
                if (epClean.isNotBlank()) curUrls.add(epClean)
            }
            _favoriteUrls.value = curUrls
            _favoriteSeriesNames.value = curSeries
            prefs.edit()
                .putStringSet(KEY_FAVORITE_URLS, curUrls)
                .putStringSet(KEY_FAVORITE_SERIES, curSeries)
                .apply()
            return true
        }
    }

    fun isItemFavorite(item: M3uItem, episodes: List<M3uItem> = emptyList()): Boolean {
        return when (item.type) {
            ItemType.SERIES -> isSeriesFavorite(item, episodes)
            ItemType.MOVIE -> isMovieFavorite(item)
            ItemType.LIVE -> isFavorite(item.url)
        }
    }
}
