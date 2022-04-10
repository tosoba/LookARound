package com.lookaround.core.android.map.clustering

import java.util.*

class Cluster<T : ClusterItem>
internal constructor(
    val latitude: Double,
    val longitude: Double,
    val items: List<T>,
    private val north: Double,
    private val west: Double,
    private val south: Double,
    private val east: Double
) {
    fun contains(latitude: Double, longitude: Double): Boolean =
        longitude in west..east && latitude in south..north

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val cluster = other as Cluster<*>
        return cluster.latitude.compareTo(latitude) == 0 &&
            cluster.longitude.compareTo(longitude) == 0
    }

    override fun hashCode(): Int = Objects.hash(latitude, longitude)
}
