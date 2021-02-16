package com.lookaround.core.android.appunta.math3d

import kotlin.math.cos
import kotlin.math.sin

/**
 * The list of trigonometric values (sin and cos) of a Vector3
 */
class Trig1 {
    var sin = 0.0
        private set
    var cos = 0.0
        private set

    constructor()
    constructor(point: Vector1) {
        setVector1(point)
    }

    fun setVector1(point: Vector1) {
        sin = sin(point.v)
        cos = cos(point.v)
    }
}