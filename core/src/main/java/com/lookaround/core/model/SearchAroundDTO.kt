package com.lookaround.core.model

import java.util.*

data class SearchAroundDTO(
    val key: String,
    val lat: Double,
    val lng: Double,
    override val lastSearchedAt: Date
) : SearchDTO
