package com.lookaround.core.repo

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.SearchAroundDTO
import kotlinx.coroutines.flow.Flow

interface ISearchAroundRepo {
    suspend fun attractionsAround(lat: Double, lng: Double, radiusInMeters: Float): List<NodeDTO>

    suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO>

    suspend fun imagesAround(lat: Double, lng: Double, radiusInMeters: Float): List<String>

    fun recentSearchesAround(limit: Int, query: String?): Flow<List<SearchAroundDTO>>

    val searchesAroundCountFlow: Flow<Int>

    suspend fun getSearchesAroundCount(query: String?): Int

    suspend fun searchResults(searchId: Long): List<NodeDTO>

    suspend fun deleteSearch(id: Long)

    suspend fun deleteAll()
}
