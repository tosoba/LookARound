package com.lookaround.api.overpass

import org.junit.Test

class OverpassEndpointsTests {
    private val endpoints = OverpassEndpoints.NEW
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532

    @Test
    fun attractions() {
        endpoints
            .interpreter(OverpassQueries.findAttractions(warsawLat, warsawLng, 10_000.0))
            .execute()
    }

    @Test
    fun placesOfType() {
        endpoints
            .interpreter(
                OverpassQueries.findPlacesOfType(
                    "restaurant",
                    warsawLat,
                    warsawLng,
                    10_000.0
                )
            )
            .execute()
    }
}