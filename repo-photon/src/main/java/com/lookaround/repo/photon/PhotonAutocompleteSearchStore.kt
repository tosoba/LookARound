package com.lookaround.repo.photon

import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreBuilder
import com.github.filosganga.geogson.model.Feature
import com.lookaround.core.model.PointDTO
import com.lookaround.repo.photon.dao.AutocompleteSearchDao
import com.lookaround.repo.photon.entity.AutocompleteSearchEntity
import com.lookaround.repo.photon.entity.AutocompleteSearchInput
import com.lookaround.repo.photon.ext.namedPointsOnly
import com.lookaround.repo.photon.ext.pointDTO
import com.lookaround.repo.photon.mapper.PointEntityMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
@FlowPreview
object PhotonAutocompleteSearchStore {
    fun build(
        dao: AutocompleteSearchDao,
        endpoints: PhotonEndpoints,
        pointEntityMapper: PointEntityMapper
    ): Store<AutocompleteSearchInput, List<PointDTO>> =
        StoreBuilder.from<AutocompleteSearchInput, List<Feature>, List<PointDTO>>(
                fetcher =
                    Fetcher.of { (query, priorityLat, priorityLon) ->
                        endpoints
                            .search(query, priorityLat = priorityLat, priorityLon = priorityLon)
                            .features()
                            ?.namedPointsOnly
                            ?: emptyList()
                    },
                sourceOfTruth =
                    SourceOfTruth.of(
                        reader = { (query, priorityLat, priorityLon) ->
                            dao.selectPoints(
                                    query = query,
                                    priorityLat = priorityLat,
                                    priorityLon = priorityLon,
                                )
                                .map {
                                    it.map(pointEntityMapper::toDTO)
                                        .takeIf(Collection<*>::isNotEmpty)
                                }
                        },
                        writer = { input, points ->
                            dao.insert(
                                search = AutocompleteSearchEntity(input),
                                points =
                                    points.map(Feature::pointDTO).map(pointEntityMapper::toEntity)
                            )
                        },
                        delete = { (query, priorityLat, priorityLon) ->
                            dao.delete(
                                query = query,
                                priorityLat = priorityLat,
                                priorityLon = priorityLon,
                            )
                        },
                        deleteAll = dao::deleteAll
                    )
            )
            .build()
}
