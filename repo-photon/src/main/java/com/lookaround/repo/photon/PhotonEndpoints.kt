package com.lookaround.repo.photon

import com.github.filosganga.geogson.model.FeatureCollection
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotonEndpoints {
    @GET("/api")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("lat") priorityLat: Double? = null,
        @Query("lon") priorityLon: Double? = null,
        @Query("lang") language: String? = null
    ): FeatureCollection

    @GET("/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): FeatureCollection

    companion object {
        internal const val BASE_URL = "https://photon.komoot.io"
    }
}
