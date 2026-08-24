package com.flixclusive.provider.frenchstream.api

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.filter.FilterList
import com.flixclusive.provider.frenchstream.core.FrenchStreamClient
import com.flixclusive.provider.frenchstream.core.FrenchStreamParser

internal class FrenchStreamSearchApi(
    private val client: FrenchStreamClient,
    private val providerId: String,
) : SearchProviderApi {
    override val filters: FilterList = FilterList()

    override suspend fun search(
        query: String,
        page: Int,
        filters: FilterList,
    ): PaginatedMedia<PartialMedia> {
        if (query.isBlank()) {
            return PaginatedMedia(page = page, hasNextPage = false, totalPages = 1, results = emptyList())
        }
        val document = client.getDocument(client.searchUrl(query))
            ?: return PaginatedMedia(page = page, hasNextPage = false, totalPages = page, results = emptyList())
        val results = FrenchStreamParser.cards(document, client.mainUrl, providerId).map { card ->
            PartialMedia(
                id = card.id,
                title = card.title,
                posterImage = card.poster,
                releaseDate = card.year?.let { year -> year.toLong() },
                providerId = providerId,
                type = if (card.isSeries) MediaType.SHOW else MediaType.MOVIE,
                homePage = card.url,
            )
        }
        return PaginatedMedia(
            page = page,
            hasNextPage = results.isNotEmpty(),
            totalPages = if (results.isEmpty()) page else page + 1,
            results = results,
        )
    }
}
