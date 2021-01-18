package com.lookaround.api.overpass

import com.lookaround.api.overpass.di.DaggerOverpassComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test

@ExperimentalCoroutinesApi
class OverpassServiceTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532
    private val radius = 10_000.0f

    private val service: OverpassService = DaggerOverpassComponent
        .builder()
        .build()
        .overpassService()

    @Test
    fun attractions() {
        runBlocking { service.attractionsAround(warsawLat, warsawLng, radius) }
    }

    @Test
    fun images() {
        runBlocking { service.imagesAround(warsawLat, warsawLng, radius) }
    }

    @Test
    fun placesOfType() {
        runBlocking { service.placesOfTypeAround("restaurant", warsawLat, warsawLng, radius) }
    }
}
