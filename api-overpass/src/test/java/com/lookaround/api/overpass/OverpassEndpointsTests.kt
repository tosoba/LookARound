package com.lookaround.api.overpass

import hu.supercluster.overpasser.adapter.OverpassQueryResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OverpassEndpointsTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532

    private val endpoints: OverpassEndpoints = Retrofit.Builder()
        .baseUrl(OverpassEndpoints.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        )
        .build()
        .create(OverpassEndpoints::class.java)

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

    private fun executeInterpreter(query: String): Response<OverpassQueryResult> = endpoints
        .interpreter(query)
        .execute()
}