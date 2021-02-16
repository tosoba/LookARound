package com.lookaround.core.android.appunta.renderer.impl;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.text.TextPaint;
import android.text.TextUtils;

import com.lookaround.core.android.appunta.location.LocationFactory;
import com.lookaround.core.android.appunta.orientation.Orientation;
import com.lookaround.core.android.appunta.point.ARObject;
import com.lookaround.core.android.appunta.point.Point;
import com.lookaround.core.android.appunta.point.PointsUtil;
import com.lookaround.core.android.appunta.renderer.PointRenderer;

import java.util.ArrayList;
import java.util.List;

public class RectViewRenderer implements PointRenderer {
    private final static float baseY = 100f;
    private final static float dialogHeight = 100f;
    private final static float dialogWidth = 400f;
    private final static float textYOffset = 20f;

    public RectViewRenderer() {
    }

    public static List<Float> getTakenYsByX(ARObject thisPoint) {
        ArrayList<Float> takenYs = new ArrayList<>();
        for (ARObject object : ARObject.getObjects()) {
            if (object.equals(thisPoint) || object.getScreenY() == null) continue;
            if (thisPoint.willOverlapWith(object, dialogWidth) && !takenYs.contains(object.getScreenY())) {
                takenYs.add(object.getScreenY());
            }
        }
        return takenYs;
    }

    @Override
    public void drawPoint(Point point, Canvas canvas, Orientation orientation) {
        float y = baseY;

        ARObject object = ARObject.findByPoint(point);
        if (object != null) {
            if (object.getScreenY() != null) {
                y = object.getScreenY();
            } else {
                y = findBestY(getTakenYsByBearing(object));
                object.setScreenY(y);
            }
        }

        point.setY(y);

        RectF rect = new RectF(
                point.getX() - dialogWidth / 2,
                point.getY() - dialogHeight / 2,
                point.getX() + dialogWidth / 2,
                point.getY() + dialogHeight / 2
        );

        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        backgroundPaint.setColor(Color.parseColor("#70ffffff"));

        TextPaint textPaint = new TextPaint();//The Paint that will draw the text
        textPaint.setColor(Color.BLACK);//Change the color if your background is white!
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setLinearText(true);

        int width = (int) (rect.width() - 10); // 10 to keep some space on the right for the "..."
        CharSequence txt = TextUtils.ellipsize("The loooooong text", textPaint, width, TextUtils.TruncateAt.END);
        canvas.drawText(txt, 0, txt.length(), point.getX() - dialogWidth / 2 + textYOffset, point.getY(), textPaint);

        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint);
    }

    private List<Float> getTakenYsByBearing(ARObject thisPoint) {
        ArrayList<Float> takenYs = new ArrayList<>();
        Location userLocation = LocationFactory.createLocation(41.383873, 2.156574, 12);
        double bearingThis = PointsUtil.calculateBearing(userLocation, thisPoint.getLocation());
        for (ARObject object : ARObject.getObjects()) {
            if (object.equals(thisPoint) || object.getScreenY() == null) continue;
            double bearingCurrent = PointsUtil.calculateBearing(userLocation, object.getLocation());
            if (Math.abs(bearingCurrent - bearingThis) < 30.0 && !takenYs.contains(object.getScreenY())) {
                takenYs.add(object.getScreenY());
            }
        }
        return takenYs;
    }

    private float findBestY(List<Float> takenYs) {
        float bestY = baseY;
        while (takenYs.contains(bestY)) {
            bestY += dialogHeight;
        }
        return bestY;
    }
}
