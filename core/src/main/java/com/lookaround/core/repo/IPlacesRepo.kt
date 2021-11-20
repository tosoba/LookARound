package com.lookaround.core.repo

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.SearchAroundDTO
import kotlinx.coroutines.flow.Flow

interface IPlacesRepo {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>

    suspend fun recentSearchesAround(limit: Int): Flow<List<SearchAroundDTO>>
}
