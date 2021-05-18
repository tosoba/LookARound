package com.lookaround.repo.overpass.entity

import androidx.room.ColumnInfo
import androidx.room.Ignore
import nice.fontaine.overpass.models.query.settings.Filter
import nice.fontaine.overpass.models.response.geometries.Node

data class SearchAroundInput(
    val lat: Double,
    val lng: Double,
    @ColumnInfo(name = "radius_in_meters") val radiusInMeters: Float,
    val key: String,
    val value: String,
    val filter: Filter,
    @Ignore val transformer: (List<Node>.() -> List<Node>)?
) {
    constructor(
        lat: Double,
        lng: Double,
        radiusInMeters: Float,
        key: String,
        value: String,
        filter: Filter
    ) : this(lat, lng, radiusInMeters, key, value, filter, null)
}
