package com.lookaround.core.android.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.lookaround.core.android.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class BoxedVerticalSeekbar : View {
    /** The min value of progress value. */
    var min = 0

    /** The Maximum value that this SeekArc can be set to */
    var max = 100
        set(value) {
            require(value > min) { "Max should not be less than min value" }
            field = value
        }

    var defaultValue = 0
        set(value) {
            require(value <= max) { "Default value should not be bigger than max value." }
            field = value
        }

    /** The increment/decrement value for each movement of progress. */
    var step = 10

    /** The corner radius of the view. */
    var cornerRadius = 10
        set(value) {
            field = value
            invalidate()
        }

    /** Text size in SP. */
    private var textSize = 26f

    /** Text bottom padding in pixel. */
    private var textBottomPadding = 20
    private var points = 0
    private var enabled = true

    /** Enable or disable text . */
    private var textEnabled = true

    /** Enable or disable image . */
    var isImageEnabled = false
    var isSnapEnabled = true

    /** mTouchDisabled touches will not move the slider only swipe motion will activate it */
    private var touchDisabled = true
    private var progressSweep = 0f
    private val seekbarPaint = Paint()
    private val progressPaint: Paint = Paint()
    private val textPaint: Paint = Paint()
    private var scrWidth = 0
    private var scrHeight = 0
    var onValuesChangeListener: OnValuesChangeListener? = null
    private var defaultImage: Bitmap? = null
    private var minImage: Bitmap? = null
    private var maxImage: Bitmap? = null
    private val rect = Rect()
    private var firstRun = true
    private var pointsText = defaultValue.toString()

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val density = resources.displayMetrics.density

        // Defaults, may need to link this into theme settings.
        var progressColor = ContextCompat.getColor(context, R.color.color_progress)
        var backgroundColor = ContextCompat.getColor(context, R.color.color_background)
        var textColor = ContextCompat.getColor(context, R.color.color_text)
        textSize *= density
        defaultValue = max / 2

        if (attrs != null) {
            val styledAttrs =
                context.obtainStyledAttributes(attrs, R.styleable.BoxedVerticalSeekbar, 0, 0)
            points = styledAttrs.getInteger(R.styleable.BoxedVerticalSeekbar_points, points)
            max = styledAttrs.getInteger(R.styleable.BoxedVerticalSeekbar_max, max)
            min = styledAttrs.getInteger(R.styleable.BoxedVerticalSeekbar_min, min)
            step = styledAttrs.getInteger(R.styleable.BoxedVerticalSeekbar_step, step)
            defaultValue =
                styledAttrs.getInteger(R.styleable.BoxedVerticalSeekbar_defaultValue, defaultValue)
            cornerRadius =
                styledAttrs.getInteger(
                    R.styleable.BoxedVerticalSeekbar_libCornerRadius,
                    cornerRadius
                )
            textBottomPadding =
                styledAttrs.getInteger(
                    R.styleable.BoxedVerticalSeekbar_textBottomPadding,
                    textBottomPadding
                )

            // Images
            isImageEnabled =
                styledAttrs.getBoolean(
                    R.styleable.BoxedVerticalSeekbar_imageEnabled,
                    isImageEnabled
                )
            if (isImageEnabled) {
                defaultImage =
                    requireNotNull(
                            styledAttrs.getDrawable(
                                R.styleable.BoxedVerticalSeekbar_defaultImage
                            ) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, defaultImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
                minImage =
                    requireNotNull(
                            styledAttrs.getDrawable(R.styleable.BoxedVerticalSeekbar_minImage) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, minImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
                maxImage =
                    requireNotNull(
                            styledAttrs.getDrawable(R.styleable.BoxedVerticalSeekbar_maxImage) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, maxImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
            }

            progressColor =
                styledAttrs.getColor(R.styleable.BoxedVerticalSeekbar_progressColor, progressColor)
            backgroundColor =
                styledAttrs.getColor(
                    R.styleable.BoxedVerticalSeekbar_backgroundColor,
                    backgroundColor
                )
            textSize = styledAttrs.getDimension(R.styleable.BoxedVerticalSeekbar_textSize, textSize)
            textColor = styledAttrs.getColor(R.styleable.BoxedVerticalSeekbar_textColor, textColor)
            enabled = styledAttrs.getBoolean(R.styleable.BoxedVerticalSeekbar_enabled, enabled)
            touchDisabled =
                styledAttrs.getBoolean(
                    R.styleable.BoxedVerticalSeekbar_touchDisabled,
                    touchDisabled
                )
            textEnabled =
                styledAttrs.getBoolean(R.styleable.BoxedVerticalSeekbar_textEnabled, textEnabled)
            isSnapEnabled =
                styledAttrs.getBoolean(R.styleable.BoxedVerticalSeekbar_snapEnabled, isSnapEnabled)
            points = defaultValue
            styledAttrs.recycle()
        }

        // range check
        points = min(points, max)
        points = max(points, min)

        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true
        progressPaint.style = Paint.Style.STROKE

        textPaint.color = textColor
        textPaint.isAntiAlias = true
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = textSize

        seekbarPaint.alpha = 255
        seekbarPaint.color = backgroundColor
        seekbarPaint.isAntiAlias = true

        scrHeight = context.resources.displayMetrics.heightPixels
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        scrWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        scrHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        progressPaint.strokeWidth = scrWidth.toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.translate(0f, 0f)
        val path = Path()
        path.addRoundRect(
            RectF(0f, 0f, scrWidth.toFloat(), scrHeight.toFloat()),
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            Path.Direction.CCW
        )
        canvas.clipPath(path, Region.Op.INTERSECT)
        canvas.drawRect(0f, 0f, scrWidth.toFloat(), scrHeight.toFloat(), seekbarPaint)
        canvas.drawLine(
            width.toFloat() / 2,
            height.toFloat(),
            width.toFloat() / 2,
            progressSweep,
            progressPaint
        )
        if (isImageEnabled) {
            // If image is enabled, text will not be shown
            when (points) {
                max -> drawIcon(requireNotNull(maxImage), canvas)
                min -> drawIcon(requireNotNull(minImage), canvas)
                else -> drawIcon(requireNotNull(defaultImage), canvas)
            }
        } else {
            // If image is disabled and text is enabled show text
            if (textEnabled) {
                drawText(canvas, textPaint, pointsText)
            }
        }
        if (firstRun) {
            firstRun = false
            value = points
        }
    }

    private fun drawText(canvas: Canvas, paint: Paint, text: String) {
        canvas.getClipBounds(rect)
        val cWidth = rect.width()
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(text, 0, text.length, rect)
        val x = cWidth / 2f - rect.width() / 2f - rect.left
        canvas.drawText(text, x, (canvas.height - textBottomPadding).toFloat(), paint)
    }

    private fun drawIcon(bitmap: Bitmap, canvas: Canvas) {
        val resizedBitmap = getResizedBitmap(bitmap, canvas.width / 2, canvas.width / 2)
        canvas.drawBitmap(
            resizedBitmap,
            null,
            RectF(
                canvas.width.toFloat() / 2 - resizedBitmap.width.toFloat() / 2,
                (canvas.height - resizedBitmap.height).toFloat(),
                canvas.width.toFloat() / 3 + resizedBitmap.width,
                canvas.height.toFloat()
            ),
            null
        )
    }

    private fun getResizedBitmap(bm: Bitmap, newHeight: Int, newWidth: Int): Bitmap {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // create a matrix for the manipulation
        val matrix = Matrix()
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight)
        // recreate the new Bitmap
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (enabled) {
            this.parent.requestDisallowInterceptTouchEvent(true)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onValuesChangeListener?.onStartTrackingTouch(this)
                    if (!touchDisabled) updateOnTouch(event)
                }
                MotionEvent.ACTION_MOVE -> updateOnTouch(event)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onValuesChangeListener?.onStopTrackingTouch(this)
                    if (isSnapEnabled) value = roundedProgress
                    isPressed = false
                    this.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            return true
        }
        return false
    }

    /**
     * Update the UI components on touch events.
     *
     * @param event MotionEvent
     */
    private fun updateOnTouch(event: MotionEvent) {
        isPressed = true
        val touchPoint = convertTouchEventPoint(event.y)
        val progress = touchPoint.roundToInt()
        updateProgress(progress)
    }

    private fun convertTouchEventPoint(yPos: Float): Double =
        when {
            yPos > scrHeight * 2 -> (scrHeight * 2).toFloat()
            yPos < 0 -> 0f
            else -> yPos
        }.toDouble()

    private fun updateProgress(progress: Int) {
        progressSweep = progress.toFloat()
        val coalescedProgress = max(min(progress, scrHeight), 0)

        // convert progress to min-max range
        points = coalescedProgress * (max - min) / scrHeight + min
        // reverse value because progress is descending
        points = max + min - points
        // if value is not max or min, apply step
        if (points != max && points != min) {
            points = points - points % step + min % step
        }
        onValuesChangeListener?.onPointsChanged(this, points)
        pointsText = roundedProgress.toString()
        invalidate()
    }

    private val roundedProgress: Int
        get() = ((scrHeight - progressSweep) / scrHeight * (max - min)).roundToInt()

    /**
     * Gets a value, converts it to progress for the seekBar and updates it.
     *
     * @param value The value given
     */
    private fun updateProgressByValue(value: Int) {
        points = value
        points = min(points, max)
        points = max(points, min)

        // convert min-max range to progress
        progressSweep = ((points - min) * scrHeight / (max - min)).toFloat()
        // reverse value because progress is descending
        progressSweep = scrHeight - progressSweep
        onValuesChangeListener?.onPointsChanged(this, points)
        pointsText = value.toString()
        invalidate()
    }

    interface OnValuesChangeListener {
        fun onPointsChanged(seekbar: BoxedVerticalSeekbar, points: Int) = Unit
        fun onStartTrackingTouch(seekbar: BoxedVerticalSeekbar) = Unit
        fun onStopTrackingTouch(seekbar: BoxedVerticalSeekbar) = Unit
    }

    var value: Int
        get() = points
        set(points) {
            updateProgressByValue(max(min(points, max), min))
        }

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
