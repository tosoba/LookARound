package com.lookaround.repo.overpass.di

import android.content.Context
import com.lookaround.core.android.ext.buildRoom
import com.lookaround.core.di.annotation.TestHttpClient
import com.lookaround.core.repo.IPlacesRepo
import com.lookaround.repo.overpass.OverpassDatabase
import com.lookaround.repo.overpass.OverpassEndpoints
import com.lookaround.repo.overpass.OverpassRepo
import com.lookaround.repo.overpass.di.annotation.OverpassMoshiConverterFactory
import com.lookaround.repo.overpass.mapper.NodeMapper
import com.lookaround.repo.overpass.mapper.NodeMapperImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
abstract class OverpassModule {
    @Binds abstract fun nodeMapper(nodeMapper: NodeMapperImpl): NodeMapper

    @Binds abstract fun overpassRepo(overpassRepo: OverpassRepo): IPlacesRepo

    companion object {
        @Provides @Singleton fun nodeMapperImpl(): NodeMapperImpl = NodeMapperImpl()

        @Provides
        @Singleton
        fun overpassDatabase(@ApplicationContext context: Context): OverpassDatabase =
            context.buildRoom()

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
