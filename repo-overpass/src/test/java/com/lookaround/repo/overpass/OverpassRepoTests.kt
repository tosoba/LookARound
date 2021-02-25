package com.lookaround.repo.overpass

import com.lookaround.repo.overpass.di.DaggerOverpassComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test

@ExperimentalCoroutinesApi
class OverpassRepoTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532
    private val radius = 10_000.0f

    private val repo: OverpassRepo = DaggerOverpassComponent.builder().build().overpassService()

    @Test
    fun attractions() {
        runBlocking { repo.attractionsAround(warsawLat, warsawLng, radius) }
    }

    @Test
    fun images() {
        runBlocking { repo.imagesAround(warsawLat, warsawLng, radius) }
    }

    @Test
    fun placesOfType() {
        runBlocking { repo.placesOfTypeAround("restaurant", warsawLat, warsawLng, radius) }
    }
}
