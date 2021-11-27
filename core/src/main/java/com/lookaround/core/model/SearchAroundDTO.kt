package com.lookaround.core.model

import java.util.*

data class SearchAroundDTO(
    override val id: Long,
    val value: String,
    val lat: Double,
    val lng: Double,
    override val lastSearchedAt: Date
) : SearchDTO
