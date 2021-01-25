package com.lookaround.repo.nominatim

import fr.dudie.nominatim.client.JsonNominatimClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Test

@ExperimentalCoroutinesApi
class NominatimTests {
    @Test
    fun client() {
        val httpClient: HttpClient = HttpClients.createDefault()
        val baseUrl = "https://nominatim.openstreetmap.org/"
        val email = "therealmerengue@gmail.com"
        val nominatimClient = JsonNominatimClient(baseUrl, httpClient, email)
        val address = nominatimClient.getAddress(1.64891269513038, 48.1166561643464)
        address.addressElements.forEach { println(it.value) }
    }
}