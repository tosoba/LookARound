package com.lookaround.repo.nominatim

import com.lookaround.repo.nominatim.di.DaggerNominatimComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.apache.http.impl.client.CloseableHttpClient
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
class NominatimTests {
    private val component = DaggerNominatimComponent.builder().build()
    private val httpClient: CloseableHttpClient = component.httpClient()
    private val repo: NominatimRepo = component.nominatimRepo()

    @Test
    fun client() {
        repo.getAddress(48.1166561643464, 1.64891269513038)
            .elements
            .forEach { println("${it.key} : ${it.value}") }
    }

    @After
    fun closeClient() {
        httpClient.close()
    }
}