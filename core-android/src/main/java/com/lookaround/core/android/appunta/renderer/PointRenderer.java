package com.lookaround.core.android.appunta.renderer;

import android.graphics.Canvas;

import com.lookaround.core.android.appunta.orientation.Orientation;
import com.lookaround.core.android.appunta.point.Point;

/***
 * This interface will be used to represent all the classes able to draw a point
 */
public interface PointRenderer {
    /***
     * This method is called when the point needs be drawn. It gives the sufficient
     * information to perform all kind of drawing.
     * @param point The point being drawn
     * @param canvas The canvas where to draw
     */
    void drawPoint(Point point, Canvas canvas, Orientation orientation);
}
