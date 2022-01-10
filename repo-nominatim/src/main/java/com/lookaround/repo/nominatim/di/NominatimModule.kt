package com.lookaround.repo.nominatim.di

import com.lookaround.core.repo.IGeocodingRepo
import com.lookaround.repo.nominatim.NominatimRepo
import com.lookaround.repo.nominatim.mapper.AddressMapper
import com.lookaround.repo.nominatim.mapper.AddressMapperImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.dudie.nominatim.client.JsonNominatimClient
import javax.inject.Singleton
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

@Module
@InstallIn(SingletonComponent::class)
abstract class NominatimModule {
    @Binds abstract fun addressMapper(nodeMapper: AddressMapperImpl): AddressMapper

    @Binds abstract fun geocodingRepo(nominatimRepo: NominatimRepo): IGeocodingRepo

    companion object {
        @Provides @Reusable fun addressMapperImpl(): AddressMapperImpl = AddressMapperImpl()

        @Provides
        @Singleton
        fun nominatimHttpClient(): CloseableHttpClient = HttpClients.createDefault()

        @Provides
        @Singleton
        fun jsonNominatimClient(httpClient: CloseableHttpClient): JsonNominatimClient =
            JsonNominatimClient(
                "https://nominatim.openstreetmap.org/",
                httpClient,
                "therealmerengue@gmail.com"
            )
    }
}
