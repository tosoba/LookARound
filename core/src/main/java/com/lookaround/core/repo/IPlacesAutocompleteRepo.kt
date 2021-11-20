package com.lookaround.core.repo

import com.lookaround.core.model.AutocompleteSearchDTO
import com.lookaround.core.model.PointDTO
import kotlinx.coroutines.flow.Flow

interface IPlacesAutocompleteRepo {
    suspend fun searchPoints(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?
    ): List<PointDTO>

    fun recentAutocompleteSearches(limit: Int): Flow<List<AutocompleteSearchDTO>>

    val autocompleteSearchesCount: Flow<Int>
}
