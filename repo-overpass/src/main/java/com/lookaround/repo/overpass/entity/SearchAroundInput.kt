package com.lookaround.repo.overpass.entity

import androidx.room.ColumnInfo
import nice.fontaine.overpass.models.query.settings.Filter

data class SearchAroundInput(
    val lat: Double,
    val lng: Double,
    @ColumnInfo(name = "radius_in_meters") val radiusInMeters: Float,
    val key: String,
    val value: String,
    val filter: Filter,
)
