package com.lookaround.core.android.ar.marker

import com.lookaround.core.android.model.Marker

interface ARMarker {
    val wrapped: Marker
    var distance: Float
    var x: Float
    var y: Float
    var isSelected: Boolean
    var isDrawn: Boolean
}
