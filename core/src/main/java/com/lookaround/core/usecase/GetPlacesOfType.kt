package com.lookaround.core.usecase

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.IPlacesRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPlacesOfType @Inject constructor(private val repo: IPlacesRepo) {
    suspend operator fun invoke(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> = repo.placesOfTypeAround(placeType, lat, lng, radiusInMeters)
}
