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
public class EyeViewRenderer implements PointRenderer {
    private Bitmap selectedBitmap = null;
    private final Resources res;
    private final int selectedId;
    private int xSelectedOff;
    private int ySelectedOff;
    private final int unselectedId;
    private Bitmap unselectedBitmap;
    private int xUnselectedOff;
    private int yUnselectedOff;
    private Paint pText;
    private Paint pBlackLine;

    /***
     * Creates and object able to draw a drawable resource in a Canvas
     * @param res A resources object in order to retrieve the drawable
     * @param selectedId Id of the drawable
     */
    public EyeViewRenderer(Resources res, int selectedId, int unselectedId) {
        this.selectedId = selectedId;
        this.unselectedId = unselectedId;
        this.res = res;
    }

    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    @Override
    public void drawPoint(Point point, Canvas canvas, Orientation orientation) {
        if (selectedBitmap == null) {
            //Initialize drawing objects
            selectedBitmap = BitmapFactory.decodeResource(res, selectedId);
            unselectedBitmap = BitmapFactory.decodeResource(res, unselectedId);

            xSelectedOff = selectedBitmap.getWidth() / 2;
            ySelectedOff = selectedBitmap.getHeight() / 2;

            xUnselectedOff = unselectedBitmap.getWidth() / 2;
            yUnselectedOff = unselectedBitmap.getHeight() / 2;

            pText = new Paint(Paint.ANTI_ALIAS_FLAG);
            pText.setStyle(Paint.Style.STROKE);
            pText.setTextAlign(Paint.Align.LEFT);
            pText.setTextSize(20);
            pText.setTypeface(Typeface.SANS_SERIF);
            pText.setColor(Color.WHITE);

            pBlackLine = new Paint(Paint.ANTI_ALIAS_FLAG);
            pBlackLine.setColor(Color.BLACK);
            pBlackLine.setTextSize(20);
            pBlackLine.setTypeface(Typeface.SANS_SERIF);
            pBlackLine.setTextAlign(Paint.Align.LEFT);

        }

        point.setY(100f);

        if (point.isSelected()) {
            canvas.drawBitmap(selectedBitmap, point.getX() - xSelectedOff, point.getY() - ySelectedOff, null);
        } else {
            canvas.drawBitmap(unselectedBitmap, point.getX() - xUnselectedOff, point.getY() - yUnselectedOff, null);
        }

        canvas.rotate(315, point.getX(), point.getY());
        canvas.drawText(point.getName(), point.getX() + 35, point.getY(), pBlackLine);
        canvas.drawText(point.getName(), point.getX() + 34, point.getY() - 2, pText);
        canvas.rotate(-315, point.getX(), point.getY());
    }
}
