package com.lookaround.repo.overpass

import com.lookaround.core.android.model.Amenity
import com.lookaround.repo.overpass.di.DaggerOverpassTestComponent
import com.lookaround.repo.overpass.ext.nodesAround
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Test

@ExperimentalCoroutinesApi
class OverpassEndpointsTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532
    private val radius = 10_000.0f

    private val endpoints: OverpassEndpoints =
        DaggerOverpassTestComponent.builder().build().overpassEndpoints()

    @Test
    fun attractions() {
        runBlocking {
            endpoints.nodesAround(warsawLat, warsawLng, radius) { equal("tourism", "attraction") }
        }
    }

    @Test
    fun placesOfType() {
        runBlocking {
            endpoints.nodesAround(warsawLat, warsawLng, radius) {
                equal(Amenity.RESTAURANT.typeKey, Amenity.RESTAURANT.typeValue)
            }
        }
    }

    @Test
    fun images() {
        runBlocking {
            endpoints
                .nodesAround(warsawLat, warsawLng, radius) { ilike("image", "http") }
                .mapNotNull { it.tags?.get("image") }
        }
    }
}
