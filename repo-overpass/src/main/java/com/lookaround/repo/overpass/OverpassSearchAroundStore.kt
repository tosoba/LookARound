package com.lookaround.repo.overpass

import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreBuilder
import com.lookaround.core.model.NodeDTO
import com.lookaround.repo.overpass.dao.SearchAroundDao
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.entity.SearchAroundInput
import com.lookaround.repo.overpass.ext.nodesAround
import com.lookaround.repo.overpass.mapper.NodeEntityMapper
import com.lookaround.repo.overpass.mapper.NodeMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import nice.fontaine.overpass.models.response.geometries.Node

@ExperimentalCoroutinesApi
@FlowPreview
internal object OverpassSearchAroundStore {
    fun build(
        dao: SearchAroundDao,
        endpoints: OverpassEndpoints,
        nodeMapper: NodeMapper,
        nodeEntityMapper: NodeEntityMapper
    ): Store<SearchAroundInput, List<NodeDTO>> =
        StoreBuilder.from<SearchAroundInput, List<Node>, List<NodeDTO>>(
                fetcher =
                    Fetcher.of { (lat, lng, radiusInMeters, key, value, filter, transformer) ->
                        val nodes =
                            endpoints.nodesAround(
                                lat = lat,
                                lng = lng,
                                radiusInMeters = radiusInMeters
                            ) { tag(key, value, filter) }
                        transformer?.invoke(nodes) ?: nodes
                    },
                sourceOfTruth =
                    SourceOfTruth.of(
                        reader = { (lat, lng, radiusInMeters, key, value, filter) ->
                            dao.selectNodes(
                                    lat = lat,
                                    lng = lng,
                                    radiusInMeters = radiusInMeters,
                                    key = key,
                                    value = value,
                                    filter = filter
                                )
                                .map {
                                    it.map(nodeEntityMapper::toDTO)
                                        .takeIf(Collection<*>::isNotEmpty)
                                }
                        },
                        writer = { input, nodes ->
                            if (nodes.isEmpty()) return@of
                            dao.insert(
                                search = SearchAroundEntity(input),
                                nodes = nodes.map(nodeMapper::toEntity)
                            )
                        },
                        delete = { (lat, lng, radiusInMeters, key, value, filter) ->
                            dao.delete(
                                lat = lat,
                                lng = lng,
                                radiusInMeters = radiusInMeters,
                                key = key,
                                value = value,
                                filter = filter
                            )
                        },
                        deleteAll = dao::deleteAll
                    )
            )
            .build()
}
