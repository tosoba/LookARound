package com.lookaround.api.overpass

import hu.supercluster.overpasser.adapter.OverpassQueryResult
import hu.supercluster.overpasser.adapter.OverpassService
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassEndpoints {
    @GET("/api/interpreter")
    fun interpreter(@Query("data") data: String): Call<OverpassQueryResult>

    companion object {
        private const val BASE_URL = "https://www.overpass-api.de"

        val NEW: OverpassService
            get() = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OverpassService::class.java)
    }
}
