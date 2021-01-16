package com.lookaround.api.overpass

import nice.fontaine.overpass.models.response.OverpassResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassEndpoints {
    @GET("/api/interpreter")
    fun interpreter(@Query("data", encoded = true) data: String): Call<OverpassResponse>

    companion object {
        internal const val BASE_URL = "https://www.overpass-api.de"
    }
}
