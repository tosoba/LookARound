package com.lookaround.core.android.ar.renderer

import android.graphics.Canvas
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation

interface MarkerRenderer {
    fun draw(marker: ARMarker, canvas: Canvas, orientation: Orientation)
}
