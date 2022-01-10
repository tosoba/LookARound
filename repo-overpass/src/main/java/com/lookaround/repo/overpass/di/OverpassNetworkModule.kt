package com.lookaround.repo.overpass.di

import com.lookaround.repo.overpass.OverpassEndpoints
import com.lookaround.repo.overpass.di.annotation.OverpassMoshiConverterFactory
import com.lookaround.repo.overpass.mapper.NodeEntityMapper
import com.lookaround.repo.overpass.mapper.NodeEntityMapperImpl
import com.lookaround.repo.overpass.mapper.NodeMapper
import com.lookaround.repo.overpass.mapper.NodeMapperImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.*
import javax.inject.Singleton
import nice.fontaine.overpass.models.response.adapters.ElementAdapter
import nice.fontaine.overpass.models.response.adapters.Iso8601Adapter
import nice.fontaine.overpass.models.response.adapters.MemberAdapter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class OverpassNetworkModule {
    @Binds abstract fun nodeMapper(mapper: NodeMapperImpl): NodeMapper

    @Binds abstract fun nodeEntityMapper(mapper: NodeEntityMapperImpl): NodeEntityMapper

    companion object {
        @Provides @Reusable fun nodeMapperImpl(): NodeMapperImpl = NodeMapperImpl()

        @Provides
        @Reusable
        fun nodeEntityMapperImpl(): NodeEntityMapperImpl = NodeEntityMapperImpl()

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
            httpClient: OkHttpClient,
        ): OverpassEndpoints =
            Retrofit.Builder()
                .baseUrl(OverpassEndpoints.BASE_URL)
                .addConverterFactory(converterFactory)
                .client(httpClient)
                .build()
                .create(OverpassEndpoints::class.java)
    }
}
