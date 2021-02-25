package com.lookaround.core.android.appunta.marker

import com.lookaround.core.android.appunta.renderer.MarkerRenderer
import com.lookaround.core.android.model.Marker

interface CameraMarker {
    val marker: Marker
    var distance: Double
    var renderer: MarkerRenderer?
    var x: Float
    var y: Float
    var isSelected: Boolean
    var isDrawn: Boolean
}
