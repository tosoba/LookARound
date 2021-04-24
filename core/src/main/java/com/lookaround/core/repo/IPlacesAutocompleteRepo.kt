package com.lookaround.core.repo

import com.lookaround.core.model.PointDTO

interface IPlacesAutocompleteRepo {
    suspend fun searchPoints(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?
    ): List<PointDTO>
}
