package com.flixclusive.provider.frenchstream.api

import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.PaginatedMedia
import com.flixclusive.model.provider.Catalog
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.frenchstream.core.FrenchStreamClient
import com.flixclusive.provider.frenchstream.core.FrenchStreamParser

internal class FrenchStreamCatalogApi(
    private val client: FrenchStreamClient,
    private val providerId: String,
) : CatalogProviderApi {
    private val catalogs = listOf(
        Catalog(name = "Derniers films", url = "films", canPaginate = true, providerId = providerId),
        Catalog(name = "Dernières séries", url = "s-tv", canPaginate = true, providerId = providerId),
        Catalog(name = "Films populaires", url = "films/top-film", canPaginate = true, providerId = providerId),
        Catalog(name = "Séries du moment", url = "sries-du-moment", canPaginate = true, providerId = providerId),
        Catalog(name = "Films d’action", url = "films/actions", canPaginate = true, providerId = providerId),
        Catalog(name = "Films comédie", url = "films/comedies", canPaginate = true, providerId = providerId),
        Catalog(name = "Films fantastique", url = "films/fantastiques", canPaginate = true, providerId = providerId),
        Catalog(name = "Films science-fiction", url = "films/science-fictions", canPaginate = true, providerId = providerId),
        Catalog(name = "Séries Netflix", url = "s-tv/netflix-series-", canPaginate = true, providerId = providerId),
        Catalog(name = "Séries Disney+", url = "s-tv/series-disney-plus", canPaginate = true, providerId = providerId),
    )

    override suspend fun getCatalogs(): List<Catalog> = catalogs

    override suspend fun getCatalogItems(
        catalog: Catalog,
        page: Int,
    ): PaginatedMedia<PartialMedia> {
        val suffix = if (page > 1) "${catalog.url}/page/$page" else catalog.url
        val document = client.getDocument(client.siteUrl(suffix))
            ?: return PaginatedMedia(page = page, hasNextPage = false, totalPages = page, results = emptyList())
        val results = FrenchStreamParser.cards(document, client.mainUrl, providerId).map { card ->
            PartialMedia(
                id = card.id,
                title = card.title,
                posterImage = card.poster,
                releaseDate = card.year?.toLong(),
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
