package com.lookaround.core.android.ext

import android.graphics.PointF
import com.mapzen.tangram.CameraUpdateFactory
import com.mapzen.tangram.MapController
import com.mapzen.tangram.MapView
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
        httpHandler
    )
}

fun MapController.zoomOnDoubleTap(
    zoomIncrement: Float = 1f,
    durationMs: Int = 500,
    easeType: MapController.EaseType = MapController.EaseType.CUBIC
) {
    touchInput.setDoubleTapResponder { x, y ->
        val tappedPosition = screenPositionToLngLat(PointF(x, y))
            ?: return@setDoubleTapResponder false
        updateCameraPosition(
            CameraUpdateFactory.newCameraPosition(cameraPosition.apply {
                longitude = .5 * (tappedPosition.longitude + longitude)
                latitude = .5 * (tappedPosition.latitude + latitude)
                zoom += zoomIncrement
            }),
            durationMs,
            easeType
        )
        true
    }
}
