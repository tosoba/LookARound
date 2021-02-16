package com.lookaround.core.android.appunta.renderer.impl;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
public class DrawablePointRenderer implements PointRenderer {
    private final Resources res;
    private final int id;
    private Bitmap bitmap = null;
    private int xOff;
    private int yOff;
    private Paint pText;

    /***
     * Creates and object able to draw a drawable resource in a Canvas
     * @param res A resources object in order to retrieve the drawable
     * @param id Id of the drawable
     */
    public DrawablePointRenderer(Resources res, int id) {
        this.id = id;
        this.res = res;
    }

    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    @Override
    public void drawPoint(Point point, Canvas canvas, Orientation orientation) {
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(res, id);
            xOff = bitmap.getWidth() / 2;
            yOff = bitmap.getHeight() / 2;

            pText = new Paint(Paint.ANTI_ALIAS_FLAG);
            pText.setStyle(Paint.Style.FILL);
            pText.setTextAlign(Paint.Align.LEFT);
            pText.setTextSize(20);
            pText.setTypeface(Typeface.SANS_SERIF);
            pText.setColor(Color.WHITE);
        }

        canvas.drawBitmap(bitmap, point.getX() - xOff, point.getY() - yOff, null);
        canvas.drawText(point.getName(), point.getX() + xOff, point.getY() + 8, pText);
    }
}
