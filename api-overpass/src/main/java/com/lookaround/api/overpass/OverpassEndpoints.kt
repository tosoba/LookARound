package com.lookaround.api.overpass

import hu.supercluster.overpasser.adapter.OverpassQueryResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassEndpoints {
    @GET("/api/interpreter")
    fun interpreter(@Query("data", encoded = true) data: String): Call<OverpassQueryResult>

    companion object {
        internal const val BASE_URL = "https://www.overpass-api.de"
    }
}
