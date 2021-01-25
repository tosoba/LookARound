package com.lookaround.core.repo.overpass

import com.lookaround.core.model.overpass.NodeDTO

interface IOverpassRepo {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>
}
