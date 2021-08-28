package com.lookaround.repo.photon.di

import com.github.filosganga.geogson.gson.GeometryAdapterFactory
import com.google.gson.GsonBuilder
import com.lookaround.repo.photon.PhotonEndpoints
import com.lookaround.repo.photon.mapper.PointEntityMapper
import com.lookaround.repo.photon.mapper.PointEntityMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotonNetworkModule {
    @Binds abstract fun pointEntityMapper(mapper: PointEntityMapperImpl): PointEntityMapper

    companion object {
        @Provides
        @Singleton
        fun pointEntityMapper(): PointEntityMapperImpl = PointEntityMapperImpl()

        @Provides
        @Singleton
        @PhotonGsonConverterFactory
        fun gsonConverterFactory(): GsonConverterFactory =
            GsonConverterFactory.create(
                GsonBuilder().registerTypeAdapterFactory(GeometryAdapterFactory()).create()
            )

        @Provides
        @Singleton
        fun photonEndpoints(
            @PhotonGsonConverterFactory converterFactory: GsonConverterFactory,
            httpClient: OkHttpClient
        ): PhotonEndpoints =
            Retrofit.Builder()
                .baseUrl(PhotonEndpoints.BASE_URL)
                .addConverterFactory(converterFactory)
                .client(httpClient)
                .build()
                .create(PhotonEndpoints::class.java)
    }
}
