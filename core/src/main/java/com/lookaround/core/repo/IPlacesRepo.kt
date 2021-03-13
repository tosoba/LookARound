package com.lookaround.core.repo

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO

interface IPlacesRepo {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>
}
