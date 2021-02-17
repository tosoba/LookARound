package com.lookaround.core.android.appunta.point

import android.location.Location
import com.lookaround.core.android.appunta.renderer.PointRenderer
import java.util.*

/***
 * A single point representing a place, it contains information on where it's
 * located in space, in screen, it's id and name and the name of the renderer to
 * use to draw it.
 */
class SimplePoint(
    override var id: UUID,
    override var location: Location,
    override var name: String,
    override var renderer: PointRenderer? = null
) : Point {
    override var x = 0f
    override var y = 0f
    override var distance = 0.0
    override var isSelected = false
    override var isDrawn = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as SimplePoint
        return that.id == id
    }

    override fun hashCode(): Int = id.hashCode()
}
