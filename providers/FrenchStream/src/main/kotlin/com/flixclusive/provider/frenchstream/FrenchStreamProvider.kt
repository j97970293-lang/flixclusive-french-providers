package com.flixclusive.provider.frenchstream

import android.content.Context
import com.flixclusive.provider.FlixclusiveProvider
import com.flixclusive.provider.ProviderPlugin
import com.flixclusive.provider.capability.CatalogProviderApi
import com.flixclusive.provider.capability.MediaLinkProviderApi
import com.flixclusive.provider.capability.MediaMetadataProviderApi
import com.flixclusive.provider.capability.SearchProviderApi
import com.flixclusive.provider.frenchstream.api.FrenchStreamCatalogApi
import com.flixclusive.provider.frenchstream.api.FrenchStreamLinkApi
import com.flixclusive.provider.frenchstream.api.FrenchStreamMetadataApi
import com.flixclusive.provider.frenchstream.api.FrenchStreamSearchApi
import com.flixclusive.provider.frenchstream.core.FrenchStreamClient

@FlixclusiveProvider
class FrenchStreamProvider : ProviderPlugin() {
    private val client by lazy { FrenchStreamClient() }
    private val catalogApi by lazy { FrenchStreamCatalogApi(client, manifest.id) }
    private val searchApi by lazy { FrenchStreamSearchApi(client, manifest.id) }
    private val metadataApi by lazy { FrenchStreamMetadataApi(client, manifest.id) }
    private val linkApi by lazy { FrenchStreamLinkApi(client) }

    override suspend fun getCatalogApi(context: Context): CatalogProviderApi = catalogApi

    override suspend fun getSearchApi(context: Context): SearchProviderApi = searchApi

    override suspend fun getMetadataApi(context: Context): MediaMetadataProviderApi = metadataApi

    override suspend fun getMediaLinkApi(context: Context): MediaLinkProviderApi = linkApi
}
