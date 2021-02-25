package com.lookaround.core.android.appunta.renderer

import android.graphics.Canvas
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.marker.CameraMarker

interface MarkerRenderer {
    fun drawPoint(cameraMarker: CameraMarker, canvas: Canvas, orientation: Orientation)
}
