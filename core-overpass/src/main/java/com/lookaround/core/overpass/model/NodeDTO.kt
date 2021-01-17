package com.lookaround.core.overpass.model

data class NodeDTO(val id: Long, val lat: Double, val lon: Double, val tags: Map<String, String>)
