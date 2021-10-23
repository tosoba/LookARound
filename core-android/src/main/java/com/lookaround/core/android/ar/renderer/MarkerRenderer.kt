package com.lookaround.core.android.ar.renderer

import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation

interface MarkerRenderer {
    val markerWidthPx: Float
        get() = DEFAULT_MARKER_DIMENSION_PX
    val markerHeightPx: Float
        get() = DEFAULT_MARKER_DIMENSION_PX

    fun draw(markers: List<ARMarker>, canvas: Canvas, orientation: Orientation): List<RectF>
    fun postDraw(drawnRects: List<RectF>) = Unit

    fun onSaveInstanceState(): Bundle? = null
    fun onRestoreInstanceState(bundle: Bundle?) = Unit

    companion object {
        const val DEFAULT_MARKER_DIMENSION_PX = 50f
    }
}
