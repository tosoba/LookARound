package com.lookaround.repo.photon.di

import com.github.filosganga.geogson.gson.GeometryAdapterFactory
import com.google.gson.GsonBuilder
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.repo.photon.PhotonEndpoints
import com.lookaround.repo.photon.PhotonRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotonModule {
    @Binds abstract fun placesAutoCompleteRepo(repo: PhotonRepo): IPlacesAutocompleteRepo

    companion object {
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
