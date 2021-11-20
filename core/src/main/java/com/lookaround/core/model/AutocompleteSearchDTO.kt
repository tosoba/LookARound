package com.lookaround.core.model

import java.util.*

data class AutocompleteSearchDTO(
    override val id: Long,
    val query: String,
    val priorityLat: Double?,
    val priorityLon: Double?,
    override val lastSearchedAt: Date,
) : SearchDTO
