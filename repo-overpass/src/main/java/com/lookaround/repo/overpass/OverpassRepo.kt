package com.lookaround.repo.overpass

import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.IPlacesRepo
import com.lookaround.repo.overpass.mapper.NodeMapper
import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.OverpassResponse
import nice.fontaine.overpass.models.response.geometries.Node
import javax.inject.Inject

class OverpassRepo
@Inject
constructor(
    private val endpoints: OverpassEndpoints,
    private val nodeMapper: NodeMapper,
) : IPlacesRepo {
    override suspend fun attractionsAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> =
        nodesAround(lat, lng, radiusInMeters) { equal("tourism", "attraction") }
            .map(nodeMapper::toDTO)

    override suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> =
        nodesAround(lat, lng, radiusInMeters) { equal(placeType.typeKey, placeType.typeValue) }
            .map(nodeMapper::toDTO)

    override suspend fun imagesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<String> =
        nodesAround(lat, lng, radiusInMeters) { ilike("image", "http") }.mapNotNull {
            it.tags?.get("image")
        }

    private suspend fun nodesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        compose: NodeQuery.Builder.() -> NodeQuery.Builder
    ): List<Node> =
        endpoints.interpreter(NodeQuery.Builder().compose().aroundQuery(lat, lng, radiusInMeters))
            .nodes

    private fun NodeQuery.Builder.aroundQuery(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): String = around(lat, lng, radiusInMeters).build().toQuery()

    private val OverpassResponse.nodes: List<Node>
        get() = elements.toList().filterIsInstance<Node>().filter { it.tags?.get("name") != null }
}
