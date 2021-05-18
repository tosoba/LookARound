package com.lookaround.repo.overpass.ext

import com.lookaround.repo.overpass.OverpassEndpoints
import nice.fontaine.overpass.models.query.statements.NodeQuery
import nice.fontaine.overpass.models.response.OverpassResponse
import nice.fontaine.overpass.models.response.geometries.Node

internal val OverpassResponse.nodes: List<Node>
    get() = elements.toList().filterIsInstance<Node>().filter { it.tags?.get("name") != null }

internal fun NodeQuery.Builder.aroundQuery(
    lat: Double,
    lng: Double,
    radiusInMeters: Float
): String = around(lat, lng, radiusInMeters).build().toQuery()

internal suspend fun OverpassEndpoints.nodesAround(
    lat: Double,
    lng: Double,
    radiusInMeters: Float,
    compose: NodeQuery.Builder.() -> NodeQuery.Builder
): List<Node> =
    interpreter(NodeQuery.Builder().compose().aroundQuery(lat, lng, radiusInMeters)).nodes
