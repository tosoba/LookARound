package com.lookaround.repo.overpass.di

import com.lookaround.core.di.annotation.TestHttpClient
import com.lookaround.repo.overpass.OverpassEndpoints
import com.lookaround.repo.overpass.di.annotation.OverpassMoshiConverterFactory
import com.lookaround.repo.overpass.mapper.NodeMapper
import com.lookaround.repo.overpass.mapper.NodeMapperImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import nice.fontaine.overpass.models.response.adapters.ElementAdapter
import nice.fontaine.overpass.models.response.adapters.Iso8601Adapter
import nice.fontaine.overpass.models.response.adapters.MemberAdapter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import javax.inject.Singleton

@Module
abstract class OverpassModule {
    @Binds abstract fun nodeMapper(nodeMapper: NodeMapperImpl): NodeMapper

    companion object {
        @Provides @Singleton fun nodeMapperImpl(): NodeMapperImpl = NodeMapperImpl()

        @Provides
        @Singleton
        @OverpassMoshiConverterFactory
        fun moshiConverterFactory(): MoshiConverterFactory =
            MoshiConverterFactory.create(
                Moshi.Builder()
                    .add(MemberAdapter())
                    .add(ElementAdapter())
                    .add(Date::class.java, Iso8601Adapter())
                    .build()
            )

        @Provides
        @Singleton
        fun overpassEndpoints(
            @OverpassMoshiConverterFactory converterFactory: MoshiConverterFactory,
            @TestHttpClient httpClient: OkHttpClient,
        ): OverpassEndpoints =
            Retrofit.Builder()
                .baseUrl(OverpassEndpoints.BASE_URL)
                .addConverterFactory(converterFactory)
                .client(httpClient)
                .build()
                .create(OverpassEndpoints::class.java)
    }
}
