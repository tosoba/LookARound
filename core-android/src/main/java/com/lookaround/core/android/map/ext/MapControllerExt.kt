package com.lookaround.core.android.map.ext

import android.graphics.PointF
import android.graphics.RectF
import com.mapzen.tangram.LngLat
import com.mapzen.tangram.MapController

fun MapController.screenAreaToBoundingBox(padding: RectF): BoundingBox? {
    val view = glViewHolder?.view ?: return null
    val w = view.width
    val h = view.height
    if (w == 0 || h == 0) return null

    val size = PointF(w - padding.left - padding.right, h - padding.top - padding.bottom)

    // the special cases here are: map tilt and map rotation:
    // * map tilt makes the screen area -> world map area into a trapezoid
    // * map rotation makes the screen area -> world map area into a rotated rectangle
    // dealing with tilt: this method is just not defined if the tilt is above a certain limit
    if (cameraPosition.tilt > Math.PI / 4f) return null // 45°

    val positions =
        arrayOf(
                screenPositionToLatLon(PointF(padding.left, padding.top)),
                screenPositionToLatLon(PointF(padding.left + size.x, padding.top)),
                screenPositionToLatLon(PointF(padding.left, padding.top + size.y)),
                screenPositionToLatLon(PointF(padding.left + size.x, padding.top + size.y))
            )
            .filterNotNull()

    return positions.enclosingBoundingBox()
}

fun MapController.screenPositionToLatLon(screenPosition: PointF): LatLon? =
    screenPositionToLngLat(screenPosition)?.toLatLon()

fun LngLat.toLatLon(): LatLon = LatLon(latitude, longitude)

fun LatLon.toLngLat(): LngLat = LngLat(longitude, latitude)

fun Iterable<LatLon>.enclosingBoundingBox(): BoundingBox {
    val it = iterator()
    require(it.hasNext()) { "positions is empty" }
    val origin = it.next()
    var minLatOffset = 0.0
    var minLonOffset = 0.0
    var maxLatOffset = 0.0
    var maxLonOffset = 0.0
    while (it.hasNext()) {
        val pos = it.next()
        // calculate with offsets here to properly handle 180th meridian
        val lat = pos.latitude - origin.latitude
        val lon = normalizeLongitude(pos.longitude - origin.longitude)
        if (lat < minLatOffset) minLatOffset = lat
        if (lon < minLonOffset) minLonOffset = lon
        if (lat > maxLatOffset) maxLatOffset = lat
        if (lon > maxLonOffset) maxLonOffset = lon
    }
    return BoundingBox(
        origin.latitude + minLatOffset,
        normalizeLongitude(origin.longitude + minLonOffset),
        origin.latitude + maxLatOffset,
        normalizeLongitude(origin.longitude + maxLonOffset)
    )
}

fun normalizeLongitude(lon: Double): Double {
    var lon = lon % 360 // lon is now -360..360
    lon = (lon + 360) % 360 // lon is now 0..360
    if (lon > 180) lon -= 360 // lon is now -180..180
    return lon
}

data class BoundingBox(val min: LatLon, val max: LatLon) {
    constructor(
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double
    ) : this(LatLon(minLatitude, minLongitude), LatLon(maxLatitude, maxLongitude))

    init {
        require(min.latitude <= max.latitude) {
            "Min latitude ${min.latitude} is greater than max latitude ${max.latitude}"
        }
    }

    val crosses180thMeridian
        get() = min.longitude > max.longitude
}

data class LatLon(val latitude: Double, val longitude: Double) {
    init {
        checkValidity(latitude, longitude)
    }

    companion object {
        fun checkValidity(latitude: Double, longitude: Double) {
            require(
                latitude >= -90.0 && latitude <= +90 && longitude >= -180 && longitude <= +180
            ) { "Latitude $latitude, longitude $longitude is not a valid position" }
        }
    }
}
