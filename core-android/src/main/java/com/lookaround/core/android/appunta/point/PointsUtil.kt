package com.lookaround.core.android.appunta.point

import android.location.Location
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/***
 * A simple utility class intented to  perform basic operations
 */
object PointsUtil {
    private const val EARTH_RADIUS_METERS = 6371000

    /***
     * Calculate the distance from a given point to all the points stored and
     * sets the distance property for all them
     *
     * @param location
     * Latitude and longitude of the given point
     */
    fun calculateDistance(points: List<Point>, location: Location) {
        points.forEach { poi -> poi.distance = distanceInMeters(poi.location, location) }
    }

    /***
     * Returns a subset of points that are below a distance of a given point
     *
     * @param location
     * Latitude and longitude of the given point
     * @param distance
     * Distance to filter
     * @return The subset list
     */
    fun getNearPoints(points: List<Point>, location: Location, distance: Double): List<Point> {
        calculateDistance(points, location)
        return points.filter { it.distance <= distance }
    }

    /**
     * Computes the distance in meters between two points on Earth.
     *
     * @param location      Latitude and longitude of the first point
     * @param otherLocation Latitude and longitude of the second point
     * @return Distance between the two points in kilometers.
     */
    private fun distanceInMeters(location: Location, otherLocation: Location): Double {
        val lat1Rad = Math.toRadians(otherLocation.latitude)
        val lat2Rad = Math.toRadians(location.latitude)
        val deltaLonRad = Math.toRadians(location.longitude - otherLocation.longitude)
        return (acos(sin(lat1Rad) * sin(lat2Rad) + (cos(lat1Rad) * cos(lat2Rad) * cos(deltaLonRad))) * EARTH_RADIUS_METERS)
    }

    fun calculateBearing(pos1: Location, pos2: Location): Double = calculateBearing(
        Math.toRadians(pos1.latitude),
        Math.toRadians(pos1.longitude),
        Math.toRadians(pos2.latitude),
        Math.toRadians(pos2.longitude)
    )

    private fun calculateBearing(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val dLon = long2 - long1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        var brng = atan2(y, x)
        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        // brng = 360 - brng // count degrees counter-clockwise - remove to make clockwise
        if (brng > 180) brng = 360 - brng
        return brng
    }
}