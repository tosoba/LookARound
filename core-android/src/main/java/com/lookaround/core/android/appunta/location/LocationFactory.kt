package com.lookaround.core.android.appunta.location

import android.location.Location

/**
 * A helper intended to build a new location from simple values
 */
object LocationFactory {
    /***
     * Creates a new location with the data provided
     *
     * @param latitude
     * Latitude of the point
     * @param longitude
     * Longitude of the point
     * @return A location object with the data provided
     */
    fun create(
        latitude: Double, longitude: Double, altitude: Double = 0.0
    ): Location = Location("").also {
        it.latitude = latitude
        it.longitude = longitude
        it.altitude = altitude
        it.accuracy = 0f
    }
}
