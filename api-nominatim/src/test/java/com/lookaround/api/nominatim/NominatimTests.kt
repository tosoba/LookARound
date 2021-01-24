package com.lookaround.api.nominatim

import fr.dudie.nominatim.client.JsonNominatimClient
import fr.dudie.nominatim.model.BoundingBox
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
        val viewBox = BoundingBox()
        viewBox.north = 48.2731
        viewBox.south = 48.2163
        viewBox.east = -4.5758
        viewBox.west = -4.4127
        val address = nominatimClient.getAddress(1.64891269513038, 48.1166561643464)
        address.addressElements.forEach { println(it.value) }
    }
}