package com.lookaround.core.overpass

import com.lookaround.core.overpass.model.NodeDTO

interface IOverpassService {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>
}
