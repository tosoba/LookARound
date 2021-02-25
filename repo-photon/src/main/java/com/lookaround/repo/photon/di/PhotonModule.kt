package com.lookaround.repo.photon.di

import com.github.filosganga.geogson.gson.GeometryAdapterFactory
import com.google.gson.GsonBuilder
import com.lookaround.core.di.annotation.TestHttpClient
import com.lookaround.repo.photon.PhotonEndpoints
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
object PhotonModule {
    @Provides
    @Singleton
    @PhotonGsonConverterFactory
    fun gsonConverterFactory(): GsonConverterFactory =
        GsonConverterFactory.create(
            GsonBuilder().registerTypeAdapterFactory(GeometryAdapterFactory()).create())

    @Provides
    @Singleton
    fun photonEndpoints(
        @PhotonGsonConverterFactory converterFactory: GsonConverterFactory,
        @TestHttpClient httpClient: OkHttpClient
    ) =
        Retrofit.Builder()
            .baseUrl(PhotonEndpoints.BASE_URL)
            .addConverterFactory(converterFactory)
            .client(httpClient)
            .build()
            .create(PhotonEndpoints::class.java)
}
