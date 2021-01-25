package com.lookaround.core.model

data class AddressDTO(
    val placeId: Long,
    val osmId: String?,
    val osmType: String?,
    val lat: Double,
    val lng: Double,
    val elements: Map<String, String>,
    val details: Map<String, String>,
    val importance: Double
)
