package com.lookaround.core.android.ar.math3d

import kotlin.math.cos
import kotlin.math.sin

/** The list of trigonometric values (sin and cos) of a Vector3 */
class Trig1 {
    var sin = 0.0
        private set
    var cos = 0.0
        private set

    fun setVector1(vector: Vector1) {
        sin = sin(vector.v)
        cos = cos(vector.v)
    }
}
