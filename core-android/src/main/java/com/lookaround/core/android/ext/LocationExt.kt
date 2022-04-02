package com.lookaround.core.android.ext

import android.location.Location
import kotlin.math.roundToInt

fun Location.preciseFormattedDistanceTo(other: Location): String =
    distanceTo(other).preciseFormattedDistance

val Float.preciseFormattedDistance: String
    get() =
        if (this >= 1_000) "${(this / 1_000).roundToDecimalPlaces(1)} km" else "${roundToInt()} m"

fun Location.roundedFormattedDistanceTo(other: Location): String =
    distanceTo(other).roundedFormattedDistance

val Float.roundedFormattedDistance: String
    get() =
        when {
            this >= 1_000 -> "${(this / 1_000).roundToDecimalPlaces(1)} km"
            this >= 100 -> "${roundToInt()} m"
            else -> "Here"
        }
