package com.lookaround.core.android.appunta.marker

import android.location.Location
import com.lookaround.core.android.appunta.renderer.MarkerRenderer
import com.lookaround.core.android.model.Marker

class SimpleCameraMarker(override val marker: Marker) : CameraMarker {
    override var x = 0f
    override var y = 0f
    override var distance = 0.0
    override var isSelected = false
    override var isDrawn = true
    override var renderer: MarkerRenderer? = null

    constructor(location: Location, name: String) : this(Marker(name, location))
}
