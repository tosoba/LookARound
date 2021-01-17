package com.lookaround.api.overpass

import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import nice.fontaine.overpass.models.response.adapters.ElementAdapter
import nice.fontaine.overpass.models.response.adapters.Iso8601Adapter
import nice.fontaine.overpass.models.response.adapters.MemberAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*


@ExperimentalCoroutinesApi
class OverpassServiceTests {
    private val warsawLat = 52.237049
    private val warsawLng = 21.017532
    private val radius = 10_000.0f

    private val moshi = Moshi.Builder()
        .add(MemberAdapter())
        .add(ElementAdapter())
        .add(Date::class.java, Iso8601Adapter())
        .build()

    private val service: OverpassService = OverpassService(
        Retrofit.Builder()
            .baseUrl(OverpassEndpoints.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
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
        runBlocking { service.findAttractions(warsawLat, warsawLng, radius) }
    }

    @Test
    fun images() {
        runBlocking { service.findImages(warsawLat, warsawLng, radius) }
    }

    @Test
    fun placesOfType() {
        runBlocking { service.findPlacesOfType("restaurant", warsawLat, warsawLng, radius) }
    }
}
