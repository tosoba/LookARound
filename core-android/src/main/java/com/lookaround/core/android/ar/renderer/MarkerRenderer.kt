package com.lookaround.core.android.ar.renderer

import android.graphics.Canvas
import android.os.Bundle
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation

interface MarkerRenderer {
    val markerWidth: Float
        get() = DEFAULT_MARKER_DIMENSION
    val markerHeight: Float
        get() = DEFAULT_MARKER_DIMENSION

    fun draw(marker: ARMarker, canvas: Canvas, orientation: Orientation)
    fun postDrawAll() = Unit

    fun onSaveInstanceState(): Bundle? = null
    fun onRestoreInstanceState(bundle: Bundle?) = Unit

    companion object {
        const val DEFAULT_MARKER_DIMENSION = 50f
    }
}
