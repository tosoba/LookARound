package com.lookaround.core.android.appunta.renderer.impl;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.lookaround.core.android.appunta.orientation.Orientation;
import com.lookaround.core.android.appunta.point.Point;
import com.lookaround.core.android.appunta.renderer.PointRenderer;

/***
 * A simple Point renderer used as default by the compass
 */
public class SimplePointRenderer implements PointRenderer {
    @Override
    public void drawPoint(Point point, Canvas canvas, Orientation orientation) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0x44FFFFFF);
        canvas.drawCircle(point.getX(), point.getY(), 5, p);
        p.setColor(0x33FFFFFF);
        canvas.drawCircle(point.getX(), point.getY(), 4, p);
        p.setColor(0x66FFFFFF);
        canvas.drawCircle(point.getX(), point.getY(), 3, p);
        p.setColor(0x99FFFFFF);
        canvas.drawCircle(point.getX(), point.getY(), 2, p);
        p.setColor(0xFFFFFFFF);
        canvas.drawCircle(point.getX(), point.getY(), 1, p);
    }
}
