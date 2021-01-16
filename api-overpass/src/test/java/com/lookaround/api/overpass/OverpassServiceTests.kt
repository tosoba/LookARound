package com.lookaround.api.overpass

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OverpassServiceTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532
    private val radius = 10_000.0f

    private val service: OverpassService = OverpassService(
        Retrofit.Builder()
            .baseUrl(OverpassEndpoints.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(
                        HttpLoggingInterceptor()
                            .apply { level = HttpLoggingInterceptor.Level.BODY }
                    )
                    .build()
            )
            .build()
            .create(OverpassEndpoints::class.java)
    )

    @Test
    fun attractions() {
        service.findAttractions(warsawLat, warsawLng, radius).execute()
    }

    @Test
    fun images() {
        service.findImages(warsawLat, warsawLng, radius).execute()
    }

    @Test
    fun placesOfType() {
        service.findPlacesOfType("restaurant", warsawLat, warsawLng, radius).execute()
    }
}
