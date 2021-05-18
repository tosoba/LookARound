package com.lookaround.core.android.di

import com.lookaround.core.android.map.MapTilesCacheConfig
import com.mapzen.tangram.networking.DefaultHttpHandler
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Module
@InstallIn(SingletonComponent::class)
object MapModule {
    @Provides
    @Singleton
    fun glViewHolderFactory(): GLViewHolderFactory = GLSurfaceViewHolderFactory()

    @Provides
    @Singleton
    fun mapTilesHttpHandler(tilesCacheConfig: MapTilesCacheConfig): HttpHandler =
        object : DefaultHttpHandler(OkHttpClient.Builder().cache(tilesCacheConfig.cache)) {
            override fun configureRequest(url: HttpUrl, builder: Request.Builder) {
                builder
                    .cacheControl(tilesCacheConfig.cacheControl)
                    .header("User-Agent", MapTilesCacheConfig.USER_AGENT_HEADER)
            }
        }
}
