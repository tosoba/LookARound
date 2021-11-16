package com.lookaround.core.model

sealed interface LocationDataDTO {
    data class Success(val lat: Double, val lng: Double, val alt: Double) : LocationDataDTO
    object Failure : LocationDataDTO
}
