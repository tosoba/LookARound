package com.lookaround.core.android.ext

import android.location.Location
import kotlin.math.roundToInt

fun Location.formattedDistanceTo(other: Location): String = distanceTo(other).formattedDistance

val Float.formattedDistance: String
    get() =
        if (this >= 1_000) {
            "${(this / 1_000).roundToDecimalPlaces(1)} km away"
        } else {
            "${roundToInt()} m away"
        }
