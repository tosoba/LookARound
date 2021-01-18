package com.lookaround.api.overpass.di

import com.lookaround.api.overpass.OverpassEndpoints
import com.lookaround.api.overpass.di.annotation.OverpassHttpClient
import com.lookaround.api.overpass.di.annotation.OverpassMoshiConverterFactory
import com.lookaround.api.overpass.mapper.NodeMapper
import com.lookaround.api.overpass.mapper.NodeMapperImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import nice.fontaine.overpass.models.response.adapters.ElementAdapter
import nice.fontaine.overpass.models.response.adapters.Iso8601Adapter
import nice.fontaine.overpass.models.response.adapters.MemberAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import javax.inject.Singleton

@Module
abstract class OverpassModule {
    @Binds
    abstract fun nodeMapper(nodeMapper: NodeMapperImpl): NodeMapper

    companion object {
        @Provides
        @Singleton
        fun nodeMapperImpl(): NodeMapperImpl = NodeMapperImpl()

        @Provides
        @Singleton
        @OverpassMoshiConverterFactory
        fun moshiConverterFactory(): MoshiConverterFactory = MoshiConverterFactory.create(
            Moshi.Builder()
                .add(MemberAdapter())
                .add(ElementAdapter())
                .add(Date::class.java, Iso8601Adapter())
                .build()
        )

        @Provides
        @Singleton
        @OverpassHttpClient
        fun overpassHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) //TODO: add interceptor if BuildConfig.DEBUG only
                .build()

        @Provides
        @Singleton
        fun overpassEndpoints(
            @OverpassMoshiConverterFactory converterFactory: MoshiConverterFactory,
            @OverpassHttpClient httpClient: OkHttpClient,
        ): OverpassEndpoints = Retrofit.Builder()
            .baseUrl(OverpassEndpoints.BASE_URL)
            .addConverterFactory(converterFactory)
            .client(httpClient)
            .build()
            .create(OverpassEndpoints::class.java)
    }
}
