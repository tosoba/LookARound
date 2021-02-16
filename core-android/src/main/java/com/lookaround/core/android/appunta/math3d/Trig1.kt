package com.lookaround.core.android.appunta.math3d;

/**
 * The list of trigonometric values (sin and cos) of a Vector3
 */
public class Trig1 {
    public double sin;
    public double cos;

    public Trig1() {
    }

    public Trig1(Vector1 point) {
        setVector1(point);
    }

    public void setVector1(Vector1 point) {
        sin = Math.sin(point.v);
        cos = Math.cos(point.v);
    }
}
