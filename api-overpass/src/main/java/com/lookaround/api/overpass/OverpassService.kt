package com.lookaround.api.overpass

import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.OverpassResponse
import nice.fontaine.overpass.models.response.geometries.Node

class OverpassService(private val endpoints: OverpassEndpoints) {
    suspend fun findAttractions(
        lat: Double, lng: Double, radiusInMeters: Float
    ): List<Node> = nodesAround(lat, lng, radiusInMeters) { equal("tourism", "attraction") }

    suspend fun findPlacesOfType(
        type: String, lat: Double, lng: Double, radiusInMeters: Float
    ): List<Node> = nodesAround(lat, lng, radiusInMeters) { equal("amenity", type) }

    suspend fun findImages(
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
