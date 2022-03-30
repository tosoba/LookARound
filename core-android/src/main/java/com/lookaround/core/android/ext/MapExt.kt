package com.lookaround.core.android.ext

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import androidx.annotation.DrawableRes
import com.lookaround.core.android.R
import com.lookaround.core.android.map.model.BoundingBox
import com.lookaround.core.android.map.model.LatLon
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object GetMapException : Throwable()

suspend fun MapView.init(
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory
): MapController = suspendCoroutine { continuation ->
    getMapAsync(
        { mapController ->
            mapController?.let { continuation.resume(it) }
                ?: continuation.resumeWithException(GetMapException)
        },
        glViewHolderFactory,
        httpHandler
    )
}

fun MapController.zoomOnDoubleTap(
    zoomIncrement: Float = 1f,
    durationMs: Int = 50,
    easeType: MapController.EaseType = MapController.EaseType.CUBIC
) {
    touchInput.setDoubleTapResponder { x, y ->
        val tappedPosition =
            screenPositionToLngLat(PointF(x, y)) ?: return@setDoubleTapResponder false
        updateCameraPosition(
            CameraUpdateFactory.newCameraPosition(
                cameraPosition.apply {
                    longitude = .5 * (tappedPosition.longitude + longitude)
                    latitude = .5 * (tappedPosition.latitude + latitude)
                    zoom += zoomIncrement
                }
            ),
            durationMs,
            easeType
        )
        true
    }
}

suspend fun MapController.captureFrame(waitForTilesLoaded: Boolean = true): Bitmap =
    suspendCoroutine {
        captureFrame(it::resume, waitForTilesLoaded)
    }

private const val PREF_ROTATION = "map_rotation"
private const val PREF_TILT = "map_tilt"
private const val PREF_ZOOM = "map_zoom"
private const val PREF_LAT = "map_lat"
private const val PREF_LON = "map_lon"

fun MapController.saveCameraPosition(outState: Bundle) {
    with(outState) {
        putFloat(PREF_ROTATION, cameraPosition.rotation)
        putFloat(PREF_TILT, cameraPosition.tilt)
        putFloat(PREF_ZOOM, cameraPosition.zoom)
        putDouble(PREF_LAT, cameraPosition.position.latitude)
        putDouble(PREF_LON, cameraPosition.position.longitude)
    }
}

fun MapController.restoreCameraPosition(savedInstanceState: Bundle) {
    updateCameraPosition(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition().apply {
                latitude = savedInstanceState.getDouble(PREF_LAT)
                longitude = savedInstanceState.getDouble(PREF_LON)
                rotation = savedInstanceState.getFloat(PREF_ROTATION)
                tilt = savedInstanceState.getFloat(PREF_TILT)
                zoom = savedInstanceState.getFloat(PREF_ZOOM)
            }
        )
    )
}

fun MapController.moveCameraPositionTo(lat: Double, lng: Double, zoom: Float, durationMs: Int = 0) {
    if (cameraPosition.latitude == lat &&
            cameraPosition.longitude == lng &&
            cameraPosition.zoom == zoom
    ) {
        return
    }

    updateCameraPosition(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition().also {
                it.latitude = lat
                it.longitude = lng
                it.zoom = zoom
            }
        ),
        durationMs
    )
}

fun MapController.screenAreaToBoundingBox(padding: RectF = RectF()): BoundingBox? {
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
    screenPositionToLngLat(screenPosition)?.latLon

fun MapController.addMarkerFor(
    location: Location,
    stylingString: String =
        "{ style: 'points', size: [27px, 27px], order: 2000, collide: false, color: blue, interactive: true}",
    @DrawableRes drawableId: Int = R.drawable.ic_map_marker
): Marker =
    addMarker().apply {
        setPoint(LngLat(location.longitude, location.latitude))
        isVisible = true
        setStylingFromString(stylingString)
        setDrawable(drawableId)
    }

val LngLat.latLon: LatLon
    get() = LatLon(latitude, longitude)

val LatLon.lngLat: LngLat
    get() = LngLat(longitude, latitude)

val Location.latLon: LatLon
    get() = LatLon(latitude, longitude)

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
    var result = lon % 360 // lon is now -360..360
    result = (result + 360) % 360 // lon is now 0..360
    if (result > 180) result -= 360 // lon is now -180..180
    return result
}

class MarkerPickResult(val position: LatLon, val uuid: UUID? = null)

typealias TangramMarker = Marker

typealias TangramMarkerPickResult = com.mapzen.tangram.MarkerPickResult

fun locationWith(latitude: Double, longitude: Double, altitude: Double = 0.0): Location =
    Location("").also {
        it.latitude = latitude
        it.longitude = longitude
        it.altitude = altitude
    }
