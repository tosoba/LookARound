package com.lookaround.core.android.appunta.point

import android.location.Location
import java.util.*

class ARObject(val point: Point) {
    var screenY: Float? = null

    val location: Location
        get() = point.location

    companion object {
        val objects: MutableMap<UUID, ARObject> = HashMap()
        fun getObjects(): Collection<ARObject> = objects.values
        fun findByPoint(point: Point): ARObject? = objects[point.id]
    }

    init {
        if (!objects.containsKey(point.id)) objects[point.id] = this
    }
}