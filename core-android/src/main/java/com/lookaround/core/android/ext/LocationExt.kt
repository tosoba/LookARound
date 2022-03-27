package com.lookaround.core.android.ext

import android.location.Location
import kotlin.math.roundToInt

fun Location.formattedDistanceTo(other: Location): String = distanceTo(other).formattedDistance

val Float.formattedDistance: String
    get() =
        when {
            this >= 1_000 -> "${(this / 1_000).roundToDecimalPlaces(1)} km"
            this >= 100 -> "${roundToInt()} m"
            else -> "Here"
        }
