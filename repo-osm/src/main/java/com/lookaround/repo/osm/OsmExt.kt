package com.lookaround.repo.osm

import kotlin.math.*

fun latLngZoomToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
    val latRad = Math.toRadians(lat)
    var xtile = floor((lon + 180) / 360 * (1 shl zoom)).toInt()
    var ytile = floor((1.0 - asinh(tan(latRad)) / PI) / 2 * (1 shl zoom)).toInt()
    if (xtile < 0) xtile = 0
    if (xtile >= (1 shl zoom)) xtile = (1 shl zoom) - 1
    if (ytile < 0) ytile = 0
    if (ytile >= (1 shl zoom)) ytile = (1 shl zoom) - 1
    return xtile to ytile
}
