package com.lookaround.repo.overpass

import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.lookaround.core.model.NodeDTO
import com.lookaround.repo.overpass.dao.SearchAroundDao
import com.lookaround.repo.overpass.entity.SearchAroundEntity
import com.lookaround.repo.overpass.ext.nodes
import com.lookaround.repo.overpass.mapper.NodeEntityMapper
import com.lookaround.repo.overpass.mapper.NodeMapper
import com.lookaround.repo.overpass.model.SearchAroundInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import nice.fontaine.overpass.models.query.settings.Filter
import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.geometries.Node

@ExperimentalCoroutinesApi
@FlowPreview
internal object OverpassSearchAroundStore {
    private fun NodeQuery.Builder.aroundQuery(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): String = around(lat, lng, radiusInMeters).build().toQuery()

    private suspend fun OverpassEndpoints.nodesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        compose: NodeQuery.Builder.() -> NodeQuery.Builder
    ): List<Node> =
        interpreter(NodeQuery.Builder().compose().aroundQuery(lat, lng, radiusInMeters)).nodes

    fun get(
        dao: SearchAroundDao,
        endpoints: OverpassEndpoints,
        nodeMapper: NodeMapper,
        nodeEntityMapper: NodeEntityMapper
    ) {
        StoreBuilder.from<SearchAroundInput, List<Node>, List<NodeDTO>>(
                Fetcher.of {
                    (
                        lat: Double,
                        lng: Double,
                        radiusInMeters: Float,
                        key: String,
                        value: String,
                        filter: Filter) ->
                    endpoints.nodesAround(lat, lng, radiusInMeters) { tag(key, value, filter) }
                },
                sourceOfTruth =
                    SourceOfTruth.of(
                        reader = {
                            (
                                lat: Double,
                                lng: Double,
                                radiusInMeters: Float,
                                key: String,
                                value: String,
                                filter: Filter) ->
                            dao.selectNodes(lat, lng, radiusInMeters, key, value, filter).map {
                                it.map(nodeEntityMapper::toDTO)
                            }
                        },
                        writer = { input, nodes ->
                            dao.insert(
                                search = SearchAroundEntity(input),
                                nodes = nodes.map(nodeMapper::toEntity)
                            )
                        },
                        delete = {
                            (
                                lat: Double,
                                lng: Double,
                                radiusInMeters: Float,
                                key: String,
                                value: String,
                                filter: Filter) ->
                            dao.delete(lat, lng, radiusInMeters, key, value, filter)
                        },
                        deleteAll = dao::deleteAll
                    )
            )
            .build()
    }
}
