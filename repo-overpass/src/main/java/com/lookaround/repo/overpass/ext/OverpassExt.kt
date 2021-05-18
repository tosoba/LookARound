package com.lookaround.repo.overpass.ext

import nice.fontaine.overpass.models.response.OverpassResponse
import nice.fontaine.overpass.models.response.geometries.Node

internal val OverpassResponse.nodes: List<Node>
    get() = elements.toList().filterIsInstance<Node>().filter { it.tags?.get("name") != null }