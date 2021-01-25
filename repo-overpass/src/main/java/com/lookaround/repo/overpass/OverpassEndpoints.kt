package com.lookaround.repo.overpass

import nice.fontaine.overpass.models.response.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassEndpoints {
    @GET("/api/interpreter")
    suspend fun interpreter(@Query("data", encoded = true) data: String): OverpassResponse

    companion object {
        internal const val BASE_URL = "https://www.overpass-api.de"
    }
}
