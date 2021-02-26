package com.lookaround.core.android.ar.math3d

import kotlin.math.cos
import kotlin.math.sin

/** The list of trigonometric values (sin and cos) of a Vector3 */
class Trig3 {
    var xSin = 0.0
        private set
    var xCos = 0.0
        private set
    var ySin = 0.0
        private set
    var yCos = 0.0
        private set
    var zSin = 0.0
        private set
    var zCos = 0.0
        private set

    /**
     * Stores the trigonometric values of a 3d vector
     *
     * @param vector The set of angles used to calculate trigonometric values
     */
    fun setVector3(vector: Vector3) {
        xSin = sin(vector.x)
        ySin = sin(vector.y)
        zSin = sin(vector.z)
        xCos = cos(vector.x)
        yCos = cos(vector.y)
        zCos = cos(vector.z)
    }
}
