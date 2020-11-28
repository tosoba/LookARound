package com.lookaround.api.overpass

import org.junit.Test

class OverpassEndpointsTests {
    private val endpoints = OverpassEndpoints.NEW
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532

    @Test
    fun attractions() {
        executeInterpreter(
            OverpassQueries.findAttractions(warsawLat, warsawLng, 10_000.0)
        )
    }

    @Test
    fun placesOfType() {
        executeInterpreter(
            OverpassQueries.findPlacesOfType(
                "restaurant",
                warsawLat,
                warsawLng,
                10_000.0
            )
        )
    }

    private fun executeInterpreter(query: String) {
        endpoints.interpreter(query).execute()
    }
}