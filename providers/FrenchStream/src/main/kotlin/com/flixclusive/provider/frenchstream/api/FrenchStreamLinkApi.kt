package com.flixclusive.provider.frenchstream.api

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.MediaLink
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaLinkType
import com.flixclusive.provider.frenchstream.core.FrenchStreamClient
import com.flixclusive.provider.frenchstream.core.FrenchStreamParser
import java.net.URI

internal class FrenchStreamLinkApi(
    private val client: FrenchStreamClient,
) : MediaLinkProviderApi {
    override val supportedLinkTypes: Set<MediaLinkType> = setOf(
        MediaLinkType.STREAMS,
        MediaLinkType.SUBTITLES,
    )

    override suspend fun getLinks(
        media: MediaMetadata,
        episode: Episode?,
        onLinkFound: (MediaLink) -> Unit,
    ) {
        val pageUrl = episode?.id
            ?.takeIf { it.startsWith(FrenchStreamClient.EPISODE_API_MARKER) }
            ?.removePrefix(FrenchStreamClient.EPISODE_API_MARKER)
            ?: media.homePage
            ?: return

        val candidates = mutableListOf<Pair<String?, String>>()
        val contentId = Regex("(?:newsid=|/)([0-9]{3,})(?:[-.]|$)").find(pageUrl)?.groupValues?.getOrNull(1)
        if (contentId != null && episode == null) {
            val api = client.siteUrl("/engine/ajax/film_api.php?id=$contentId")
            client.getJson(api)?.let { candidates += FrenchStreamParser.movieLinks(it) }
        }

        if (candidates.isEmpty()) {
            val document = client.getDocument(pageUrl)
            if (document != null) {
                document.select("a[href]").forEach { anchor ->
                    val href = anchor.attr("href").trim()
                    if (href.startsWith("http")) {
                        val language = anchor.text().takeIf { text ->
                            text.contains("VF", true) || text.contains("VOST", true)
                        }?.let { text -> if (text.contains("VOST", true)) "VOSTFR" else "VF" }
                        candidates += language to href
                    }
                }
                FrenchStreamParser.directMediaUrls(document.html()).forEach { direct ->
                    candidates += null to direct
                }
            }
        }

        if (candidates.isEmpty()) return

        candidates.distinctBy { it.second }.forEach { (language, candidate) ->
            val normalized = FrenchStreamParser.absolute(candidate, pageUrl)
            when {
                normalized.endsWith(".vtt", true) || normalized.endsWith(".srt", true) -> {
                    onLinkFound(Subtitle(url = normalized, language = language ?: "fr"))
                }
                normalized.contains(".m3u8", true) || normalized.contains(".mp4", true) -> {
                    onLinkFound(
                        Stream(
                            url = normalized,
                            name = streamName(normalized, language),
                            description = "Lecteur direct FrenchStream",
                        )
                    )
                }
                else -> resolvePlayer(normalized, language, onLinkFound)
            }
        }
    }

    private suspend fun resolvePlayer(
        url: String,
        language: String?,
        onLinkFound: (MediaLink) -> Unit,
    ) {
        val html = client.getText(url) ?: return
        FrenchStreamParser.directMediaUrls(html).forEach { direct ->
            val normalized = FrenchStreamParser.absolute(direct, url)
            if (normalized.endsWith(".vtt", true) || normalized.endsWith(".srt", true)) {
                onLinkFound(Subtitle(url = normalized, language = language ?: "fr"))
            } else {
                onLinkFound(
                    Stream(
                        url = normalized,
                        name = streamName(url, language),
                        description = "Lecteur ${host(url)}",
                    )
                )
            }
        }
    }

    private fun streamName(url: String, language: String?): String {
        val prefix = language?.takeIf { it.isNotBlank() }?.uppercase()?.plus(" ").orEmpty()
        return prefix + host(url).replaceFirstChar { it.uppercase() }
    }

    private fun host(url: String): String {
        return runCatching { URI(url).host.orEmpty().substringBefore('.') }
            .getOrDefault("FrenchStream")
            .ifBlank { "FrenchStream" }
    }
}
