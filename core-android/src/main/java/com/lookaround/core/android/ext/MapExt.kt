package com.lookaround.core.android.ext

import android.graphics.PointF
import android.os.Bundle
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object GetMapException : Throwable()

suspend fun MapView.init(
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory = GLSurfaceViewHolderFactory()
): MapController = suspendCoroutine { continuation ->
    getMapAsync(
        { mapController ->
            mapController?.let { continuation.resume(it) }
                ?: continuation.resumeWithException(GetMapException)
        },
        glViewHolderFactory,
        httpHandler)
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
                }),
            durationMs,
            easeType)
        true
    }
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

fun MapController.restoreCameraPosition(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) return
    updateCameraPosition(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition().apply {
                latitude = savedInstanceState.getDouble(PREF_LAT)
                longitude = savedInstanceState.getDouble(PREF_LON)
                rotation = savedInstanceState.getFloat(PREF_ROTATION)
                tilt = savedInstanceState.getFloat(PREF_TILT)
                zoom = savedInstanceState.getFloat(PREF_ZOOM)
            }))
}
