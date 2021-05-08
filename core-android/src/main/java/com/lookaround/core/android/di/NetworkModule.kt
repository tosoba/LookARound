package com.lookaround.core.android.di

import com.lookaround.core.android.map.MapTilesCacheConfig
import com.lookaround.core.di.CoreNetworkModule
import com.mapzen.tangram.networking.DefaultHttpHandler
import com.mapzen.tangram.networking.HttpHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.beryukhov.reactivenetwork.ReactiveNetwork

@Module(includes = [CoreNetworkModule::class])
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun reactiveNetwork(): ReactiveNetwork = ReactiveNetwork()

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
