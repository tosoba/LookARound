package com.lookaround.core.android.appunta.renderer.impl;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.lookaround.core.android.appunta.orientation.Orientation;
import com.lookaround.core.android.appunta.point.Point;
import com.lookaround.core.android.appunta.renderer.PointRenderer;

/***
 * This class is used to generate a PointRenderer using a drawable
 * resource
 */
public class AugmentedDrawablePointRenderer implements PointRenderer {
    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    @Override
    public void drawPoint(Point point, Canvas canvas, Orientation orientation) {
        Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setStyle(Paint.Style.STROKE);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTextSize(20);
        pText.setTypeface(Typeface.SANS_SERIF);
        pText.setColor(Color.WHITE);

        Paint pCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCircle.setStyle(Paint.Style.STROKE);
        pCircle.setStrokeWidth(4);
        pCircle.setColor(Color.WHITE);

        Paint pBlackLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBlackLine.setColor(Color.BLACK);
        pBlackLine.setTextSize(20);
        pBlackLine.setTypeface(Typeface.SANS_SERIF);
        pBlackLine.setTextAlign(Paint.Align.CENTER);

        float size = (float) ((10 - point.getDistance()) * 6);
        canvas.drawCircle(point.getX(), point.getY(), size, pCircle);
        float textWidth = (float) pText.breakText(point.getName(), true, 500, null) / 2;
        canvas.drawText(point.getName(), point.getX() - textWidth + 2, point.getY() + size + 16, pBlackLine);
        canvas.drawText(point.getName(), point.getX() - textWidth, point.getY() + size + 14, pText);
    }
}
