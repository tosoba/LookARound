package com.lookaround.repo.nominatim.di

import com.lookaround.repo.nominatim.NominatimRepo
import dagger.Component
import javax.inject.Singleton
import org.apache.http.impl.client.CloseableHttpClient

@Singleton
@Component(modules = [NominatimModule::class])
interface NominatimComponent {
    fun nominatimRepo(): NominatimRepo
    fun httpClient(): CloseableHttpClient
}
