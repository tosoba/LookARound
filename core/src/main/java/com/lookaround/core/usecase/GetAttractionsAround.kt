package com.lookaround.core.usecase

import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.ISearchAroundRepo
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetAttractionsAround @Inject constructor(private val repo: ISearchAroundRepo) {
    suspend operator fun invoke(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO> =
        repo.attractionsAround(lat = lat, lng = lng, radiusInMeters = radiusInMeters)
}
