package com.lookaround.core.android.map.clustering

internal class QuadTreeNode<T : QuadTreePoint>(
    north: Double,
    west: Double,
    south: Double,
    east: Double,
    private val bucketSize: Int
) {
    private val bounds: QuadTreeRect = QuadTreeRect(north, west, south, east)
    private val points: MutableList<T> = ArrayList(bucketSize)
    private var northWest: QuadTreeNode<T>? = null
    private var northEast: QuadTreeNode<T>? = null
    private var southWest: QuadTreeNode<T>? = null
    private var southEast: QuadTreeNode<T>? = null

    fun insert(point: T): Boolean {
        // Ignore objects that do not belong in this quad tree.
        if (!bounds.contains(point.latitude, point.longitude)) return false

        // If there is space in this quad tree, add the object here.
        if (points.size < bucketSize) {
            points.add(point)
            return true
        }

        // Otherwise, subdivide and then add the point to whichever node will accept it.
        if (northWest == null) subdivide()
        if (northWest?.insert(point) == true) return true
        if (northEast?.insert(point) == true) return true
        if (southWest?.insert(point) == true) return true
        return southEast?.insert(point) ?: false

        // Otherwise, the point cannot be inserted for some unknown reason (this should never
        // happen).
    }

    fun queryRange(range: QuadTreeRect, pointsInRange: MutableList<T>) {
        // Automatically abort if the range does not intersect this quad.
        if (!bounds.intersects(range)) return

        // Check objects at this quad level.
        for (point in points) {
            if (range.contains(point.latitude, point.longitude)) {
                pointsInRange.add(point)
            }
        }

        // Terminate here, if there are no children.
        if (northWest == null) return

        // Otherwise, add the points from the children.
        northWest?.queryRange(range, pointsInRange)
        northEast?.queryRange(range, pointsInRange)
        southWest?.queryRange(range, pointsInRange)
        southEast?.queryRange(range, pointsInRange)
    }

    private fun subdivide() {
        val northSouthHalf = bounds.north - (bounds.north - bounds.south) / 2.0
        val eastWestHalf = bounds.east - (bounds.east - bounds.west) / 2.0
        northWest =
            QuadTreeNode(bounds.north, bounds.west, northSouthHalf, eastWestHalf, bucketSize)
        northEast =
            QuadTreeNode(bounds.north, eastWestHalf, northSouthHalf, bounds.east, bucketSize)
        southWest =
            QuadTreeNode(northSouthHalf, bounds.west, bounds.south, eastWestHalf, bucketSize)
        southEast =
            QuadTreeNode(northSouthHalf, eastWestHalf, bounds.south, bounds.east, bucketSize)
    }
}
