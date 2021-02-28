package com.lookaround.core.android.ar.marker

import android.location.Location
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import com.lookaround.core.android.model.Marker
import java.util.*

class SimpleARMarker(override val wrapped: Marker) : ARMarker {
    override var x = 0f
    override var y = 0f
    override var distance = 0.0
    override var isSelected = false
    override var isDrawn = true
    override var renderer: MarkerRenderer? = null

    constructor(name: String, location: Location) : this(Marker(name, location))

    override fun equals(other: Any?): Boolean =
        this === other || (other is SimpleARMarker && other.wrapped == wrapped)

    override fun hashCode(): Int = Objects.hash(wrapped)
}
