package com.lookaround.core.android.ext

import android.location.Location
import kotlin.math.roundToInt

fun Location.formattedDistanceTo(other: Location): String {
    val distanceMeters = distanceTo(other)
    return if (distanceMeters >= 1_000) {
        "${(distanceMeters / 1_000).roundToDecimalPlaces(1)} km away"
    } else {
        "${distanceMeters.roundToInt()} m away"
    }
}
