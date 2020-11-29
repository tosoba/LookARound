package com.lookaround.api.overpass

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import hu.supercluster.overpasser.adapter.OverpassQueryResult
import org.junit.Test
import retrofit2.Response

class OverpassEndpointsTests {
    private val endpoints = OverpassEndpoints.NEW
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532

    @Test
    fun attractions() {
        val response = executeInterpreter(
            OverpassQueries.findAttractions(warsawLat, warsawLng, 10_000.0)
        )

        println(response.body()?.elements?.size ?: 0)
        val warsawLL = LatLng(warsawLat, warsawLng)
        val filtered = response.body()
            ?.elements
            ?.filter {
                SphericalUtil.computeDistanceBetween(warsawLL, LatLng(it.lat, it.lon)) <= 10_000.0
            }
        println(filtered?.size ?: 0)
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

    private fun executeInterpreter(query: String): Response<OverpassQueryResult> = endpoints
        .interpreter(query)
        .execute()
}