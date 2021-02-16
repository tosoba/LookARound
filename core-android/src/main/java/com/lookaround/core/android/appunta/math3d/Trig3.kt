package com.lookaround.core.android.appunta.math3d

import kotlin.math.cos
import kotlin.math.sin

/**
 * The list of trigonometric values (sin and cos) of a Vector3
 *
 */
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

    constructor()

    /**
     * Constructor that prefills values based on a Vector3
     *
     * @param point The set of angles used to calculate trigonometric values
     */
    constructor(point: Vector3) {
        setVector3(point)
    }

    /**
     * Stores the trigonometric values of a 3d vector
     *
     * @param point The set of angles used to calculate trigonometric values
     */
    fun setVector3(point: Vector3) {
        xSin = sin(point.x)
        ySin = sin(point.y)
        zSin = sin(point.z)
        xCos = cos(point.x)
        yCos = cos(point.y)
        zCos = cos(point.z)
    }
}