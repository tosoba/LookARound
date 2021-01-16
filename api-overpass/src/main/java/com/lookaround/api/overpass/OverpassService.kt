package com.lookaround.api.overpass

import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.OverpassResponse
import retrofit2.Call

class OverpassService(private val endpoints: OverpassEndpoints) {
    fun findAttractions(
        lat: Double, lng: Double, radiusInMeters: Float
    ): Call<OverpassResponse> = endpoints.interpreter(
        NodeQuery.Builder()
            .equal("tourism", "attraction")
            .around(lat, lng, radiusInMeters)
            .build()
            .toQuery()
    )

    fun findPlacesOfType(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): Call<OverpassResponse> = endpoints.interpreter(
        NodeQuery.Builder()
            .equal("amenity", type)
            .around(lat, lng, radiusInMeters)
            .build()
            .toQuery()
    )

    fun findImages(
        lat: Double, lng: Double, radiusInMeters: Float
    ): Call<OverpassResponse> = endpoints.interpreter(
        NodeQuery.Builder()
            .ilike("image", "http")
            .around(lat, lng, radiusInMeters)
            .build()
            .toQuery()
    )
}
