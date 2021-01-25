package com.lookaround.core.repo

import com.lookaround.core.model.NodeDTO

interface PlacesRepo {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>
}
