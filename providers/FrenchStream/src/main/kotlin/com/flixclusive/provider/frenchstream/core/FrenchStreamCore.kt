package com.flixclusive.provider.frenchstream.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

internal data class FrenchCard(
    val id: String,
    val title: String,
    val url: String,
    val poster: String?,
    val year: Int?,
    val isSeries: Boolean,
)

internal data class FrenchEpisode(
    val season: Int,
    val number: Int,
    val title: String,
    val url: String,
)

internal class FrenchStreamClient(
    private val http: OkHttpClient = OkHttpClient.Builder().build(),
) {
    private val mirrors = listOf(
        "https://french-stream.one",
        "https://french-stream.pink",
        "https://fstream.info",
    )

    @Volatile
    var mainUrl: String = mirrors.first()
        private set

    suspend fun getText(url: String): String? = withContext(Dispatchers.IO) {
        val candidates = candidates(url)
        for ((candidate, origin) in candidates) {
            val response = runCatching {
                http.newCall(
                    Request.Builder()
                        .url(candidate)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.7")
                        .build()
                ).execute()
            }.getOrNull() ?: continue
            response.use {
                if (it.isSuccessful) {
                    mainUrl = origin
                    return@withContext it.body?.string()
                }
            }
        }
        null
    }

    suspend fun getDocument(url: String) = getText(url)?.let(Jsoup::parse)

    suspend fun getJson(url: String): JSONObject? = getText(url)?.let { text ->
        runCatching { JSONObject(text) }.getOrNull()
    }

    fun siteUrl(path: String): String = if (path.startsWith("http")) path else "$mainUrl/${path.trimStart('/')}"

    fun searchUrl(query: String): String = siteUrl(
        "/?do=search&subaction=search&story=${URLEncoder.encode(query.trim(), "UTF-8")}"
    )

    private fun candidates(url: String): List<Pair<String, String>> {
        val uri = runCatching { URI(url) }.getOrNull() ?: return listOf(url to mainUrl)
        val path = uri.rawPath.orEmpty() + uri.rawQuery?.let { "?$it" }.orEmpty()
        val requestedOrigin = "${uri.scheme}://${uri.authority}"
        val origins = (listOf(requestedOrigin, mainUrl) + mirrors).distinct()
        return origins.map { it + path to it }
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Android) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
        const val MOVIE_API_MARKER = "FS_MOVIE|"
        const val EPISODE_API_MARKER = "FS_EPISODE|"
    }
}

internal object FrenchStreamParser {
    private val directMediaRegex = Regex(
        "https?://[^\\\"'\\s<>]+?\\.(?:m3u8|mp4)(?:\\?[^\\\"'\\s<>]*)?",
        RegexOption.IGNORE_CASE,
    )
    private val idRegex = Regex("(?:newsid=|/)([0-9]{3,})(?:[-.]|$)")
    private val seasonRegex = Regex("(?:saison|season|s)[\\s._-]*([0-9]{1,2})", RegexOption.IGNORE_CASE)
    private val episodeRegex = Regex("(?:épisode|episode|ep|e)[\\s._-]*([0-9]{1,3})", RegexOption.IGNORE_CASE)

    fun cards(document: org.jsoup.nodes.Document, baseUrl: String, providerId: String): List<FrenchCard> {
        return document.select("div.short, article, .short-item, .movie-item").mapNotNull { element ->
            card(element, baseUrl)
        }.distinctBy { if (it.isSeries) "s:${normalize(it.title)}" else "m:${it.url}" }
    }

    fun card(element: Element, baseUrl: String): FrenchCard? {
        val anchor = element.selectFirst("a.short-poster, a[href]") ?: return null
        val url = absolute(anchor.attr("href"), baseUrl)
        val title = anchor.attr("title").ifBlank {
            anchor.attr("aria-label").ifBlank {
                element.selectFirst(".short-title, .title, h2, h3")?.text().orEmpty()
            }
        }.trim().ifBlank { return null }
        val image = element.selectFirst("img")
        val poster = image?.attr("data-src").orEmpty().ifBlank { image?.attr("src").orEmpty() }
            .takeUnless { it.startsWith("data:") || it.isBlank() }
            ?.let { absolute(it, baseUrl) }
        val year = Regex("\\b(19|20)\\d{2}\\b").find(element.text())?.value?.toIntOrNull()
        val isSeries = url.contains("/series/", true) || url.contains("/s-tv/", true) ||
            title.contains("saison", true) || element.text().contains("série", true)
        return FrenchCard(
            id = idFrom(url) ?: url,
            title = normalize(title),
            url = url,
            poster = poster,
            year = year,
            isSeries = isSeries,
        )
    }

    fun parseEpisodes(document: org.jsoup.nodes.Document, baseUrl: String): List<FrenchEpisode> {
        val elements = document.select(".episode, .episode-item, [data-episode], .ep-item, a[href*='episode']")
        return elements.mapNotNull { element ->
            val raw = listOf(
                element.attr("data-episode"),
                element.attr("data-ep"),
                element.text(),
                element.attr("title"),
                element.attr("href"),
            ).joinToString(" ")
            val number = episodeRegex.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Regex("\\bE?(\\d{1,3})\\b", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: return@mapNotNull null
            val season = seasonRegex.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            val href = element.attr("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            FrenchEpisode(
                season = season,
                number = number,
                title = element.text().trim().ifBlank { "Épisode $number" },
                url = absolute(href, baseUrl),
            )
        }.distinctBy { it.season to it.number }
    }

    fun episodeLinks(json: JSONObject, episode: Int): List<Pair<String?, String>> {
        val result = mutableListOf<Pair<String?, String>>()
        val languageKeys = listOf("vf", "vff", "vfq", "vostfr", "vo", "fr")
        for (language in languageKeys) {
            val branch = json.optJSONObject(language) ?: continue
            val episodeObject = branch.optJSONObject(episode.toString()) ?: continue
            episodeObject.keys().forEach { host ->
                val value = episodeObject.optString(host).takeIf { it.startsWith("http") } ?: return@forEach
                result += language.uppercase(Locale.ROOT) to value
            }
        }
        walkJson(json) { value ->
            if (value.startsWith("http") && !value.contains("film_api.php") && !value.contains("ep-data.php")) {
                result += null to value
            }
        }
        return result.distinctBy { it.second }
    }

    fun movieLinks(json: JSONObject): List<Pair<String?, String>> {
        val result = mutableListOf<Pair<String?, String>>()
        walkJson(json) { value ->
            if (value.startsWith("http") && !value.contains("film_api.php") && !value.contains("ep-data.php")) {
                val language = when {
                    value.contains("vost", true) -> "VOSTFR"
                    value.contains("vf", true) -> "VF"
                    else -> null
                }
                result += language to value
            }
        }
        return result.distinctBy { it.second }
    }

    fun directMediaUrls(html: String): List<String> = directMediaRegex.findAll(html)
        .map { it.value.replace("\\/", "/") }
        .distinct()
        .toList()

    fun idFrom(url: String): String? = idRegex.find(url)?.groupValues?.getOrNull(1)

    fun normalize(title: String): String = title
        .replace(Regex("\\s*\\[(?:VF|VOSTFR|VO)\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun absolute(value: String, baseUrl: String): String {
        return runCatching { URI(baseUrl).resolve(value).toString() }.getOrDefault(value)
    }

    private fun walkJson(value: Any?, consumer: (String) -> Unit) {
        when (value) {
            is JSONObject -> value.keys().forEach { key -> walkJson(value.opt(key), consumer) }
            is JSONArray -> for (index in 0 until value.length()) walkJson(value.opt(index), consumer)
            is String -> consumer(value)
        }
    }
}
