package com.flixclusive.provider.frenchstream.api

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.frenchstream.core.FrenchStreamClient
import com.flixclusive.provider.frenchstream.core.FrenchStreamParser
import java.util.Calendar

internal class FrenchStreamMetadataApi(
    private val client: FrenchStreamClient,
    private val providerId: String,
) : MediaMetadataProviderApi {
    override suspend fun getMovie(media: PartialMedia): Movie {
        val url = media.homePage ?: client.siteUrl(media.id)
        val document = client.getDocument(url)
        val title = document?.selectFirst("h1#s-title, h1, .title")?.text()?.trim()
            .takeUnless { it.isNullOrBlank() } ?: media.title
        val poster = document?.selectFirst("div.fposter img, .poster img, img")?.let { image ->
            image.attr("data-src").ifBlank { image.attr("src") }.takeIf { it.isNotBlank() }
                ?.let { FrenchStreamParser.absolute(it, client.mainUrl) }
        } ?: media.posterImage
        val year = year(document?.text()) ?: media.releaseDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)
        }
        return Movie(
            id = media.id,
            title = FrenchStreamParser.normalize(title),
            posterImage = poster,
            releaseDate = year?.let { dateFromYear(it) },
            providerId = providerId,
            homePage = url,
        )
    }

    override suspend fun getShow(media: PartialMedia): Show {
        val url = media.homePage ?: client.siteUrl(media.id)
        val document = client.getDocument(url)
        val title = document?.selectFirst("h1#s-title, h1, .title")?.text()?.trim()
            .takeUnless { it.isNullOrBlank() } ?: media.title
        val poster = document?.selectFirst("div.fposter img, .poster img, img")?.let { image ->
            image.attr("data-src").ifBlank { image.attr("src") }.takeIf { it.isNotBlank() }
                ?.let { FrenchStreamParser.absolute(it, client.mainUrl) }
        } ?: media.posterImage
        val detected = FrenchStreamParser.parseEpisodes(document ?: org.jsoup.nodes.Document(""), client.mainUrl)
        val episodes = detected.map { item ->
            Episode(
                id = FrenchStreamClient.EPISODE_API_MARKER + item.url,
                title = item.title,
                season = item.season,
                number = item.number,
                isReleased = true,
            )
        }.sortedWith(compareBy<Episode> { it.season }.thenBy { it.number })
        val seasons = episodes.groupBy { it.season }.map { (number, seasonEpisodes) ->
            Season.Full(
                id = "${media.id}-s$number",
                title = "Saison $number",
                number = number,
                isReleased = true,
                episodes = seasonEpisodes,
            )
        }
        val year = year(document?.text()) ?: media.releaseDate?.let {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)
        }
        return Show(
            id = media.id,
            title = FrenchStreamParser.normalize(title),
            posterImage = poster,
            releaseDate = year?.let { dateFromYear(it) },
            providerId = providerId,
            homePage = url,
            totalSeasons = seasons.size,
            totalEpisodes = episodes.size,
            seasons = seasons,
        )
    }

    override suspend fun getSeason(show: Show, season: Season.Partial): Season.Full? {
        return show.seasons.firstOrNull { it.number == season.number } as? Season.Full
    }

    private fun year(text: String?): Int? = text?.let {
        Regex("\\b(19|20)\\d{2}\\b").find(it)?.value?.toIntOrNull()
    }

    private fun dateFromYear(value: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, value)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
}
