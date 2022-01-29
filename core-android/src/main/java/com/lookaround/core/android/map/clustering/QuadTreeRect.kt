package com.lookaround.core.android.map.clustering

internal class QuadTreeRect(
    val north: Double,
    val west: Double,
    val south: Double,
    val east: Double
) {
    fun contains(latitude: Double, longitude: Double): Boolean =
        longitude in west..east && latitude in south..north

    fun intersects(bounds: QuadTreeRect): Boolean =
        west <= bounds.east && east >= bounds.west && north >= bounds.south && south <= bounds.north
}
