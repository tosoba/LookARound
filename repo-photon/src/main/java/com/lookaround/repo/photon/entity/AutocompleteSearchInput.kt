package com.lookaround.repo.photon.entity

import androidx.room.ColumnInfo

data class AutocompleteSearchInput(
    val query: String,
    @ColumnInfo(name = "priority_lat") val priorityLat: Double?,
    @ColumnInfo(name = "priority_lon") val priorityLon: Double?
)
