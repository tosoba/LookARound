package com.lookaround.core.android.appunta.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.lookaround.core.android.appunta.point.Point;

public class RadarView extends AppuntaView {
    private int rotableBackground = 0;
    private float center;
    private Bitmap rotableBacgkroundBitmap;
    private double compassAngle = 0;

    public RadarView(Context context) {
        super(context);
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    RadarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /***
     * Returns the correct size of the control when needed (Basically
     * maintaining the ratio)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        int measuredHeight = getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);

        int size = Math.max(measuredWidth, measuredHeight);
        center = (float) size / 2;
        setMeasuredDimension(size, size);
    }

    @Override
    protected void calculatePointCoordinates(Point point) {
        double pointAngle = getAngle(point) + compassAngle;
        double pixelDistance = point.getDistance() * center / getMaxDistance();
        double pointY = center - pixelDistance * Math.sin(pointAngle);
        double pointX = center + pixelDistance * Math.cos(pointAngle);
        point.setX((float) pointX);
        point.setY((float) pointY);
    }

    @Override
    protected void preRender(Canvas canvas) {
        drawBackground(canvas);
        compassAngle = getOrientation().getY();
    }

    @Override
    protected void postRender(Canvas canvas) {
        Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(0xff00a0d2);
        canvas.drawCircle(center, center, 5, pointPaint);
    }

    private void drawBackground(Canvas canvas) {
        if (getRotableBackground() != 0 && getOrientation() != null) {
            Matrix transform = new Matrix();
            transform.setRectToRect(
                    new RectF(0, 0, rotableBacgkroundBitmap.getWidth(),
                            rotableBacgkroundBitmap.getHeight()),
                    new RectF(0, 0, getWidth(), getWidth()),
                    Matrix.ScaleToFit.CENTER);
            transform.preRotate((float) -(Math.toDegrees(compassAngle)),
                    (float) rotableBacgkroundBitmap.getWidth() / 2, (float) rotableBacgkroundBitmap.getHeight() / 2);
            canvas.drawBitmap(rotableBacgkroundBitmap, transform, null);
        }
    }

    public int getRotableBackground() {
        return rotableBackground;
    }

    public void setRotableBackground(int rotableBackground) {
        this.rotableBackground = rotableBackground;
        rotableBacgkroundBitmap = BitmapFactory.decodeResource(this.getResources(), rotableBackground);
    }
}