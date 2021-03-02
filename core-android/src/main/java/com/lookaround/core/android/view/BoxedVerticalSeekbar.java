package com.lookaround.core.android.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.lookaround.core.android.R;

public class BoxedVerticalSeekbar extends View {
    private static final int MAX = 100;
    private static final int MIN = 0;

    /**
     * The min value of progress value.
     */
    private int min = MIN;

    /**
     * The Maximum value that this SeekArc can be set to
     */
    private int max = MAX;

    /**
     * The increment/decrement value for each movement of progress.
     */
    private int step = 10;

    /**
     * The corner radius of the view.
     */
    private int cornerRadius = 10;

    /**
     * Text size in SP.
     */
    private float textSize = 26;

    /**
     * Text bottom padding in pixel.
     */
    private int textBottomPadding = 20;

    private int points;

    private boolean enabled = true;
    /**
     * Enable or disable text .
     */
    private boolean textEnabled = true;

    /**
     * Enable or disable image .
     */
    private boolean imageEnabled = false;

    private boolean snapEnabled = true;

    /**
     * mTouchDisabled touches will not move the slider only swipe motion will activate it
     */
    private boolean touchDisabled = true;

    private float progressSweep = 0;
    private Paint progressPaint;
    private Paint textPaint;
    private int scrWidth;
    private int scrHeight;
    private OnValuesChangeListener onValuesChangeListener;
    private int backgroundColor;
    private int defaultValue;
    private Bitmap defaultImage;
    private Bitmap minImage;
    private Bitmap maxImage;
    private final Rect rect = new Rect();
    private boolean firstRun = true;
    private String pointsText = String.valueOf(defaultValue);

    public BoxedVerticalSeekbar(Context context) {
        super(context);
        init(context, null);
    }

    public BoxedVerticalSeekbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        System.out.println("INIT");
        float density = getResources().getDisplayMetrics().density;

        // Defaults, may need to link this into theme settings
        int progressColor = ContextCompat.getColor(context, R.color.color_progress);
        backgroundColor = ContextCompat.getColor(context, R.color.color_background);
        backgroundColor = ContextCompat.getColor(context, R.color.color_background);

        int textColor = ContextCompat.getColor(context, R.color.color_text);
        textSize = (int) (textSize * density);
        defaultValue = max / 2;

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BoxedVerticalSeekbar, 0, 0);

            points = a.getInteger(R.styleable.BoxedVerticalSeekbar_points, points);
            max = a.getInteger(R.styleable.BoxedVerticalSeekbar_max, max);
            min = a.getInteger(R.styleable.BoxedVerticalSeekbar_min, min);
            step = a.getInteger(R.styleable.BoxedVerticalSeekbar_step, step);
            defaultValue = a.getInteger(R.styleable.BoxedVerticalSeekbar_defaultValue, defaultValue);
            cornerRadius = a.getInteger(R.styleable.BoxedVerticalSeekbar_libCornerRadius, cornerRadius);
            textBottomPadding =
                    a.getInteger(R.styleable.BoxedVerticalSeekbar_textBottomPadding, textBottomPadding);
            // Images
            imageEnabled = a.getBoolean(R.styleable.BoxedVerticalSeekbar_imageEnabled, imageEnabled);

            if (imageEnabled) {
                if (a.getDrawable(R.styleable.BoxedVerticalSeekbar_defaultImage) == null) {
                    throw new IllegalArgumentException(
                            "When images are enabled, defaultImage can not be null. Please assign a drawable in the layout XML file");
                }
                if (a.getDrawable(R.styleable.BoxedVerticalSeekbar_minImage) == null) {
                    throw new IllegalArgumentException(
                            "When images are enabled, minImage can not be null. Please assign a drawable in the layout XML file");
                }
                if (a.getDrawable(R.styleable.BoxedVerticalSeekbar_maxImage) == null) {
                    throw new IllegalArgumentException(
                            "When images are enabled, maxImage can not be null. Please assign a drawable in the layout XML file");
                }
                defaultImage =
                        ((BitmapDrawable) a.getDrawable(R.styleable.BoxedVerticalSeekbar_defaultImage)).getBitmap();
                minImage = ((BitmapDrawable) a.getDrawable(R.styleable.BoxedVerticalSeekbar_minImage)).getBitmap();
                maxImage = ((BitmapDrawable) a.getDrawable(R.styleable.BoxedVerticalSeekbar_maxImage)).getBitmap();
            }

            progressColor = a.getColor(R.styleable.BoxedVerticalSeekbar_progressColor, progressColor);
            backgroundColor = a.getColor(R.styleable.BoxedVerticalSeekbar_backgroundColor, backgroundColor);

            textSize = (int) a.getDimension(R.styleable.BoxedVerticalSeekbar_textSize, textSize);
            textColor = a.getColor(R.styleable.BoxedVerticalSeekbar_textColor, textColor);

            enabled = a.getBoolean(R.styleable.BoxedVerticalSeekbar_enabled, enabled);
            touchDisabled = a.getBoolean(R.styleable.BoxedVerticalSeekbar_touchDisabled, touchDisabled);
            textEnabled = a.getBoolean(R.styleable.BoxedVerticalSeekbar_textEnabled, textEnabled);
            snapEnabled = a.getBoolean(R.styleable.BoxedVerticalSeekbar_snapEnabled, snapEnabled);

            points = defaultValue;

            a.recycle();
        }

        // range check
        points = Math.min(points, max);
        points = Math.max(points, min);

        progressPaint = new Paint();
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);
        progressPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(textSize);

        scrHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        scrWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        scrHeight = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        progressPaint.setStrokeWidth(scrWidth);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = new Paint();

        paint.setAlpha(255);
        canvas.translate(0, 0);
        Path path = new Path();
        path.addRoundRect(
                new RectF(0, 0, scrWidth, scrHeight), cornerRadius, cornerRadius, Path.Direction.CCW);
        canvas.clipPath(path, Region.Op.INTERSECT);
        paint.setColor(backgroundColor);
        paint.setAntiAlias(true);
        canvas.drawRect(0, 0, scrWidth, scrHeight, paint);

        canvas.drawLine(
                canvas.getWidth() / 2,
                canvas.getHeight(),
                canvas.getWidth() / 2,
                progressSweep,
                progressPaint);

        if (imageEnabled && defaultImage != null && minImage != null && maxImage != null) {
            // If image is enabled, text will not be shown
            if (points == max) {
                drawIcon(maxImage, canvas);
            } else if (points == min) {
                drawIcon(minImage, canvas);
            } else {
                drawIcon(defaultImage, canvas);
            }
        } else {
            // If image is disabled and text is enabled show text
            if (textEnabled) {
                drawText(canvas, textPaint, pointsText);
            }
        }

        if (firstRun) {
            firstRun = false;
            setValue(points);
        }
    }

    private void drawText(Canvas canvas, Paint paint, String text) {
        canvas.getClipBounds(rect);
        int cWidth = rect.width();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.getTextBounds(text, 0, text.length(), rect);
        float x = cWidth / 2f - rect.width() / 2f - rect.left;
        canvas.drawText(text, x, canvas.getHeight() - textBottomPadding, paint);
    }

    private void drawIcon(Bitmap bitmap, Canvas canvas) {
        bitmap = getResizedBitmap(bitmap, canvas.getWidth() / 2, canvas.getWidth() / 2);
        canvas.drawBitmap(
                bitmap,
                null,
                new RectF(
                        (canvas.getWidth() / 2) - (bitmap.getWidth() / 2),
                        canvas.getHeight() - bitmap.getHeight(),
                        (canvas.getWidth() / 3) + bitmap.getWidth(),
                        canvas.getHeight()),
                null);
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        // Thanks Piyush
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (enabled) {
            this.getParent().requestDisallowInterceptTouchEvent(true);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (onValuesChangeListener != null) {
                        onValuesChangeListener.onStartTrackingTouch(this);
                    }
                    if (!touchDisabled) updateOnTouch(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateOnTouch(event);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (onValuesChangeListener != null) {
                        onValuesChangeListener.onStopTrackingTouch(this);
                    }
                    if (snapEnabled) setValue(getRoundedProgress());
                    setPressed(false);
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return true;
        }
        return false;
    }

    /**
     * Update the UI components on touch events.
     *
     * @param event MotionEvent
     */
    private void updateOnTouch(MotionEvent event) {
        setPressed(true);
        double mTouch = convertTouchEventPoint(event.getY());
        int progress = (int) Math.round(mTouch);
        updateProgress(progress);
    }

    private double convertTouchEventPoint(float yPos) {
        float wReturn;

        if (yPos > (scrHeight * 2)) {
            wReturn = scrHeight * 2;
            return wReturn;
        } else if (yPos < 0) {
            wReturn = 0;
        } else {
            wReturn = yPos;
        }

        return wReturn;
    }

    private void updateProgress(int progress) {
        progressSweep = progress;

        progress = Math.min(progress, scrHeight);
        progress = Math.max(progress, 0);

        // convert progress to min-max range
        points = progress * (max - min) / scrHeight + min;
        // reverse value because progress is descending
        points = max + min - points;
        // if value is not max or min, apply step
        if (points != max && points != min) {
            points = points - (points % step) + (min % step);
        }

        if (onValuesChangeListener != null) {
            onValuesChangeListener.onPointsChanged(this, points);
        }

        pointsText = String.valueOf(getRoundedProgress());

        invalidate();
    }

    private int getRoundedProgress() {
        return Math.round((scrHeight - progressSweep) / scrHeight * (max - min));
    }

    /**
     * Gets a value, converts it to progress for the seekBar and updates it.
     *
     * @param value The value given
     */
    private void updateProgressByValue(int value) {
        points = value;

        points = Math.min(points, max);
        points = Math.max(points, min);

        // convert min-max range to progress
        progressSweep = (points - min) * scrHeight / (max - min);
        // reverse value because progress is descending
        progressSweep = scrHeight - progressSweep;

        if (onValuesChangeListener != null) {
            onValuesChangeListener.onPointsChanged(this, points);
        }

        pointsText = String.valueOf(value);

        invalidate();
    }

    public interface OnValuesChangeListener {
        /**
         * Notification that the point value has changed.
         *
         * @param seekbar The Seekbar view whose value has changed
         * @param points  The current point value.
         */
        void onPointsChanged(BoxedVerticalSeekbar seekbar, int points);

        void onStartTrackingTouch(BoxedVerticalSeekbar seekbar);

        void onStopTrackingTouch(BoxedVerticalSeekbar seekbar);
    }

    public void setValue(int points) {
        points = Math.min(points, max);
        points = Math.max(points, min);

        updateProgressByValue(points);
    }

    public int getValue() {
        return points;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int mMax) {
        if (mMax <= min) throw new IllegalArgumentException("Max should not be less than zero");
        this.max = mMax;
    }

    public void setCornerRadius(int mRadius) {
        this.cornerRadius = mRadius;
        invalidate();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(int mDefaultValue) {
        if (mDefaultValue > max)
            throw new IllegalArgumentException("Default value should not be bigger than max value.");
        this.defaultValue = mDefaultValue;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public boolean isImageEnabled() {
        return imageEnabled;
    }

    public void setImageEnabled(boolean mImageEnabled) {
        this.imageEnabled = mImageEnabled;
    }

    public boolean isSnapEnabled() {
        return snapEnabled;
    }

    public void setSnapEnabled(boolean snapEnabled) {
        this.snapEnabled = snapEnabled;
    }

    public void setOnBoxedPointsChangeListener(OnValuesChangeListener onValuesChangeListener) {
        this.onValuesChangeListener = onValuesChangeListener;
    }
}
