package com.lookaround.core.android.appunta.math3d;

/**
 * The list of trigonometric values (sin and cos) of a Vector3
 *
 */
public class Trig3 {
    public double xSin;
    public double xCos;
    public double ySin;
    public double yCos;
    public double zSin;
    public double zCos;

    public Trig3() {

    }

    /**
     * Constructor that prefills values based on a Vector3
     *
     * @param point The set of angles used to calculate trigonometric values
     */
    public Trig3(Vector3 point) {
        setVector3(point);
    }

    /**
     * Stores the trigonometric values of a 3d vector
     *
     * @param point The set of angles used to calculate trigonometric values
     */
    public void setVector3(Vector3 point) {
        xSin = Math.sin(point.x);
        ySin = Math.sin(point.y);
        zSin = Math.sin(point.z);
        xCos = Math.cos(point.x);
        yCos = Math.cos(point.y);
        zCos = Math.cos(point.z);
    }
}
