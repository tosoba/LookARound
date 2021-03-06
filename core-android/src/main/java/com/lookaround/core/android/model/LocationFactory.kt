package com.lookaround.core.android.model

import android.location.Location

object LocationFactory {
    fun create(latitude: Double, longitude: Double, altitude: Double = 0.0): Location =
        Location("").also {
            it.latitude = latitude
            it.longitude = longitude
            it.altitude = altitude
        }
}
