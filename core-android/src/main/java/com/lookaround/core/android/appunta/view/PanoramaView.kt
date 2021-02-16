package com.lookaround.core.android.appunta.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.lookaround.core.android.appunta.point.Point;

public class PanoramaView extends AppuntaView {
    private static final double VISIBLE_DEGREES = Math.PI / 3;
    private static final double MAX_DEGREES = Math.PI * 2;

    public PanoramaView(Context context) {
        super(context);
    }

    public PanoramaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    PanoramaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void preRender(Canvas canvas) {
    }

    @Override
    protected void calculatePointCoordinates(Point point) {
        double angularDistance = angleDifference(Math.toRadians(getOrientation().getX()), MAX_DEGREES
                / 4 - getAngle(point));
        point.setX((float) ((angularDistance + VISIBLE_DEGREES / 2) * getWidth() / VISIBLE_DEGREES));
        point.setY((float) (getHeight() - getHeight() * point.getDistance() / getMaxDistance()));
    }

    @Override
    protected void postRender(Canvas canvas) {
    }

    private double angleDifference(double centered, double moved) {
        double cwDiff = cwDifference(centered, moved);
        double ccwDiff = ccwDiference(centered, moved);
        if (cwDiff < ccwDiff) {
            return cwDiff;
        } else {
            return -ccwDiff;
        }
    }

    private double cwDifference(double centered, double moved) {
        double cw = moved - centered;
        if (cw < 0) {
            cw += MAX_DEGREES;
        }
        return cw;
    }

    private double ccwDiference(double centered, double moved) {
        double ccw = centered - moved;
        if (ccw < 0) {
            ccw += MAX_DEGREES;
        }
        return ccw;
    }
}