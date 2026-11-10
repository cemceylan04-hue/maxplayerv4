package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.model.db.PlaybackProgressEntity
import com.example.parser.ItemType
import com.example.parser.M3uItem
import com.example.ui.screens.FavoritesTabContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FavoritesTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFavoritesTabContent_empty() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeTestRule.setContent {
            FavoritesTabContent(
                recentProgressList = emptyList(),
                allItems = emptyList(),
                favLive = emptyList(),
                favMovies = emptyList(),
                favSeriesGroups = emptyList(),
                allSeriesMap = emptyMap(),
                isLandscape = true,
                onPlayStream = { _, _ -> },
                onMovieClick = { _, _ -> },
                onSeriesClick = { _, _ -> }
            )
        }
    }

    @Test
    fun testFavoritesTabContent_withItems_landscape() {
        val live = listOf(
            M3uItem(title = "TRT 1", url = "http://live1.ts", type = ItemType.LIVE),
            M3uItem(title = "ATV", url = "http://live2.ts", type = ItemType.LIVE)
        )
        val movies = listOf(
            M3uItem(title = "Inception", url = "http://movie1.mp4", type = ItemType.MOVIE)
        )
        val seriesEp = listOf(
            M3uItem(title = "Breaking Bad S01 E01", url = "http://series1.mp4", type = ItemType.SERIES, seriesName = "Breaking Bad")
        )
        val seriesGroups = listOf(
            java.util.AbstractMap.SimpleEntry("Breaking Bad", seriesEp)
        )
        val progress = listOf(
            PlaybackProgressEntity(url = "http://movie1.mp4", title = "Inception", logo = "http://logo.png", positionMs = 1000L, durationMs = 2000L, type = "movie")
        )

        composeTestRule.setContent {
            FavoritesTabContent(
                recentProgressList = progress,
                allItems = live + movies + seriesEp,
                favLive = live,
                favMovies = movies,
                favSeriesGroups = seriesGroups,
                allSeriesMap = mapOf("Breaking Bad" to seriesEp),
                isLandscape = true,
                onPlayStream = { _, _ -> },
                onMovieClick = { _, _ -> },
                onSeriesClick = { _, _ -> }
            )
        }
    }

    @Test
    fun testFavoritesTabContent_withItems_portrait() {
        val live = listOf(
            M3uItem(title = "TRT 1", url = "http://live1.ts", type = ItemType.LIVE)
        )
        val movies = listOf(
            M3uItem(title = "Inception", url = "http://movie1.mp4", type = ItemType.MOVIE)
        )
        val progress = listOf(
            PlaybackProgressEntity(url = "http://movie1.mp4", title = "Inception", logo = "http://logo.png", positionMs = 1000L, durationMs = 2000L, type = "movie")
        )

        composeTestRule.setContent {
            FavoritesTabContent(
                recentProgressList = progress,
                allItems = live + movies,
                favLive = live,
                favMovies = movies,
                favSeriesGroups = emptyList(),
                allSeriesMap = emptyMap(),
                isLandscape = false,
                onPlayStream = { _, _ -> },
                onMovieClick = { _, _ -> },
                onSeriesClick = { _, _ -> }
            )
        }
    }
}
