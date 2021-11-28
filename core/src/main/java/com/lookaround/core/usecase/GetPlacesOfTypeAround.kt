package com.lookaround.core.usecase

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetPlacesOfTypeAround @Inject constructor(private val repo: ISearchAroundRepo) {
    suspend operator fun invoke(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> = repo.placesOfTypeAround(placeType, lat, lng, radiusInMeters)
}
