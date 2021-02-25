package com.lookaround.core.android.appunta.renderer.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.marker.CameraMarker
import com.lookaround.core.android.appunta.renderer.MarkerRenderer

/***
 * A simple Point renderer used as default by the compass
 */
class SimpleMarkerRenderer : MarkerRenderer {
    override fun drawPoint(cameraMarker: CameraMarker, canvas: Canvas, orientation: Orientation) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x44FFFFFF
        canvas.drawCircle(cameraMarker.x, cameraMarker.y, 5f, paint)
        paint.color = 0x33FFFFFF
        canvas.drawCircle(cameraMarker.x, cameraMarker.y, 4f, paint)
        paint.color = 0x66FFFFFF
        canvas.drawCircle(cameraMarker.x, cameraMarker.y, 3f, paint)
        paint.color = -0x66000001
        canvas.drawCircle(cameraMarker.x, cameraMarker.y, 2f, paint)
        paint.color = -0x1
        canvas.drawCircle(cameraMarker.x, cameraMarker.y, 1f, paint)
    }
}