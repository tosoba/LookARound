package com.lookaround.repo.photon

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.get
import com.lookaround.core.model.AutocompleteSearchDTO
import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.repo.photon.dao.AutocompleteSearchDao
import com.lookaround.repo.photon.entity.AutocompleteSearchInput
import com.lookaround.repo.photon.mapper.PointEntityMapper
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PhotonRepo
@Inject
constructor(
    private val store: Store<AutocompleteSearchInput, List<PointDTO>>,
    private val dao: AutocompleteSearchDao,
    private val pointEntityMapper: PointEntityMapper
) : IAutocompleteSearchRepo {
    override suspend fun search(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?
    ): List<PointDTO> =
        store.get(
            AutocompleteSearchInput(
                query = query,
                priorityLat = priorityLat,
                priorityLon = priorityLon
            )
        )

    override fun recentAutocompleteSearches(limit: Int): Flow<List<AutocompleteSearchDTO>> =
        dao.selectSearches(limit).map {
            it.map { entity ->
                val (input, lastSearchedAt) = entity
                AutocompleteSearchDTO(
                    id = entity.id,
                    query = input.query,
                    priorityLat = input.priorityLat,
                    priorityLon = input.priorityLon,
                    lastSearchedAt = lastSearchedAt
                )
            }
        }

    override val autocompleteSearchesCount: Flow<Int>
        get() = dao.selectSearchesCount()

    override suspend fun searchResults(searchId: Long): List<PointDTO> {
        dao.updateAutocompleteSearchLastSearchedAt(searchId, Date())
        return dao.selectSearchResults(autocompleteSearchId = searchId)
            .map(pointEntityMapper::toDTO)
    }
}
