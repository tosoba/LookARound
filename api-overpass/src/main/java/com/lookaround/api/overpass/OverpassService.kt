package com.lookaround.api.overpass

import com.lookaround.api.overpass.mapper.NodeMapper
import com.lookaround.core.overpass.IOverpassService
import com.lookaround.core.overpass.model.NodeDTO
import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.OverpassResponse
import nice.fontaine.overpass.models.response.geometries.Node
import javax.inject.Inject

class OverpassService @Inject constructor(
    private val endpoints: OverpassEndpoints,
    private val nodeMapper: NodeMapper,
) : IOverpassService {

    override suspend fun attractionsAround(
        lat: Double, lng: Double, radiusInMeters: Float
    ): List<NodeDTO> = nodesAround(lat, lng, radiusInMeters) { equal("tourism", "attraction") }
        .map(nodeMapper::toDTO)

    override suspend fun placesOfTypeAround(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): List<NodeDTO> = nodesAround(lat, lng, radiusInMeters) { equal("amenity", type) }
        .map(nodeMapper::toDTO)

    override suspend fun imagesAround(
        lat: Double, lng: Double, radiusInMeters: Float
    ): List<String> = nodesAround(lat, lng, radiusInMeters) { ilike("image", "http") }
        .mapNotNull { it.tags["image"] }

    private suspend fun nodesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        compose: NodeQuery.Builder.() -> NodeQuery.Builder
    ) = endpoints
        .interpreter(NodeQuery.Builder().compose().aroundQuery(lat, lng, radiusInMeters))
        .nodes

    private fun NodeQuery.Builder.aroundQuery(
        lat: Double, lng: Double, radiusInMeters: Float
    ): String = around(lat, lng, radiusInMeters)
        .build()
        .toQuery()

    private val OverpassResponse.nodes: List<Node>
        get() = elements
            .toList()
            .filterIsInstance<Node>()
}
