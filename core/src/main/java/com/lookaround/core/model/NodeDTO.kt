package com.lookaround.core.model

data class NodeDTO(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val name: String,
    val tags: Map<String, String>
)
