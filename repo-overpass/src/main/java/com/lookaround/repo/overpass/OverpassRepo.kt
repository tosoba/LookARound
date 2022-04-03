package com.lookaround.repo.overpass

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.get
import com.lookaround.core.android.ext.locationWith
import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.SearchAroundDTO
import com.lookaround.core.repo.ISearchAroundRepo
import com.lookaround.repo.overpass.dao.SearchAroundDao
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.entity.SearchAroundInput
import com.lookaround.repo.overpass.mapper.NodeEntityMapper
import dagger.Reusable
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nice.fontaine.overpass.models.query.settings.Filter

@Reusable
class OverpassRepo
@Inject
constructor(
    private val store: Store<SearchAroundInput, List<NodeDTO>>,
    private val dao: SearchAroundDao,
    private val nodeEntityMapper: NodeEntityMapper
) : ISearchAroundRepo {
    override suspend fun attractionsAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> {
        val closestSearchResults =
            closestSearchAroundResults(
                key = "tourism",
                value = "attraction",
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters
            )
        if (closestSearchResults != null) return closestSearchResults

        dao.updateSearchAroundLastSearchedAt(
            lat = lat,
            lng = lng,
            radiusInMeters = radiusInMeters,
            key = "tourism",
            value = "attraction",
            filter = Filter.EQUAL,
            date = Date()
        )
        return store.get(
            SearchAroundInput(
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters,
                key = "tourism",
                value = "attraction",
                filter = Filter.EQUAL
            )
        )
    }

    override suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> {
        val closestSearchResults =
            closestSearchAroundResults(
                key = placeType.typeKey,
                value = placeType.typeValue,
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters
            )
        if (closestSearchResults != null) return closestSearchResults

        dao.updateSearchAroundLastSearchedAt(
            lat = lat,
            lng = lng,
            radiusInMeters = radiusInMeters,
            key = placeType.typeKey,
            value = placeType.typeValue,
            filter = Filter.EQUAL,
            date = Date()
        )
        return store.get(
            SearchAroundInput(
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters,
                key = placeType.typeKey,
                value = placeType.typeValue,
                filter = Filter.EQUAL
            )
        )
    }

    private suspend fun closestSearchAroundResults(
        key: String,
        value: String,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO>? {
        val closestSearch =
            dao.selectSearchesAround(
                    radiusInMeters = radiusInMeters,
                    key = key,
                    value = value,
                    filter = Filter.EQUAL
                )
                .minByOrNull { it.distanceToInMeters(lat = lat, lng = lng) }
                ?: return null

        val distance = closestSearch.distanceToInMeters(lat = lat, lng = lng)
        if (distance <= USE_CLOSEST_RESULTS_LIMIT_METERS) {
            return store.get(
                SearchAroundInput(
                    lat = closestSearch.input.lat,
                    lng = closestSearch.input.lng,
                    radiusInMeters = radiusInMeters,
                    key = key,
                    value = value,
                    filter = Filter.EQUAL
                )
            )
        }
        return null
    }

    private fun SearchAroundEntity.distanceToInMeters(lat: Double, lng: Double): Float =
        locationWith(latitude = lat, longitude = lng)
            .distanceTo(locationWith(latitude = input.lat, longitude = input.lng))

    override suspend fun imagesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<String> =
        store.get(
                SearchAroundInput(
                    lat = lat,
                    lng = lng,
                    radiusInMeters = radiusInMeters,
                    key = "image",
                    value = "http",
                    filter = Filter.ILIKE
                ) { filterNot { it.tags?.get("image") == null } }
            )
            .mapNotNull { it.tags["image"] }

    override fun recentSearchesAround(limit: Int, query: String?): Flow<List<SearchAroundDTO>> {
        val searches =
            if (query != null) dao.selectSearches(limit, query.lowercase())
            else dao.selectSearches(limit)
        return searches.map {
            it.map { entity ->
                val (input, lastSearchedAt) = entity
                SearchAroundDTO(
                    id = entity.id,
                    value = input.value,
                    lat = input.lat,
                    lng = input.lng,
                    lastSearchedAt = lastSearchedAt
                )
            }
        }
    }

    override val searchesAroundCountFlow: Flow<Int>
        get() = dao.selectSearchesCountFlow()

    override suspend fun getSearchesAroundCount(query: String?): Int =
        if (query != null) dao.selectSearchesCount(query.lowercase()) else dao.selectSearchesCount()

    override suspend fun searchResults(searchId: Long): List<NodeDTO> {
        dao.updateSearchAroundLastSearchedAt(searchId, Date())
        return dao.selectSearchResults(searchAroundId = searchId).map(nodeEntityMapper::toDTO)
    }

    override suspend fun deleteSearch(id: Long) {
        dao.deleteById(id)
    }

    companion object {
        private const val USE_CLOSEST_RESULTS_LIMIT_METERS = 100f
    }
}
