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

class BoxedSeekbar : View {
    var min = 0
    var max = 100
        set(value) {
            require(value > min) { "Max should not be less or equal than min value" }
            field = value
        }
    var defaultValue = 0
        set(value) {
            require(value <= max) { "Default value should not be bigger than max value." }
            field = value
        }
    var step = 10
    private var currentValue = 0
    var value: Int
        get() = currentValue
        set(points) {
            updateProgressByValue(max(min(points, max), min))
        }
    private val roundedProgress: Int
        get() =
            if (orientation == Orientation.VERTICAL) {
                ((length - progressSweep) / length * (max - min)).roundToInt()
            } else {
                (progressSweep / length * (max - min)).roundToInt()
            }
    var onValueChangeListener: OnValueChangeListener? = null

    private var enabled = true
    private var firstRun = true
    private var orientation: Orientation = Orientation.VERTICAL

    private var textEnabled = true
    private var textSizeSp = 26f
    private var textBottomPaddingPx = 20
    private var pointsText = defaultValue.toString()

    var isImageEnabled = false
    var isSnapEnabled = true
    private var touchDisabled = true
    private var progressSweep = 0f
    private var cornerRadius = 10
        set(value) {
            field = value
            invalidate()
        }

    private val seekbarPaint = Paint()
    private val progressPaint: Paint = Paint()
    private val textPaint: Paint = Paint()

    private var scrWidth = 0
    private var scrHeight = 0
    private val thickness: Int
        get() = if (orientation == Orientation.VERTICAL) scrWidth else scrHeight
    private val length: Int
        get() = if (orientation == Orientation.HORIZONTAL) scrWidth else scrHeight

    private var defaultImage: Bitmap? = null
    private var minImage: Bitmap? = null
    private var maxImage: Bitmap? = null
    private val canvasClipBoundsRect = Rect()

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
        textSizeSp *= density
        defaultValue = max / 2

        if (attrs != null) {
            val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.BoxedSeekbar, 0, 0)
            orientation =
                Orientation.values()[styledAttrs.getInt(R.styleable.BoxedSeekbar_orientation, 0)]
            currentValue = styledAttrs.getInteger(R.styleable.BoxedSeekbar_points, currentValue)
            max = styledAttrs.getInteger(R.styleable.BoxedSeekbar_max, max)
            min = styledAttrs.getInteger(R.styleable.BoxedSeekbar_min, min)
            step = styledAttrs.getInteger(R.styleable.BoxedSeekbar_step, step)
            defaultValue =
                styledAttrs.getInteger(R.styleable.BoxedSeekbar_defaultValue, defaultValue)
            cornerRadius =
                styledAttrs.getInteger(R.styleable.BoxedSeekbar_libCornerRadius, cornerRadius)
            textBottomPaddingPx =
                styledAttrs.getInteger(
                    R.styleable.BoxedSeekbar_textBottomPadding,
                    textBottomPaddingPx
                )

            // Images
            isImageEnabled =
                styledAttrs.getBoolean(R.styleable.BoxedSeekbar_imageEnabled, isImageEnabled)
            if (isImageEnabled) {
                defaultImage =
                    requireNotNull(
                            styledAttrs.getDrawable(R.styleable.BoxedSeekbar_defaultImage) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, defaultImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
                minImage =
                    requireNotNull(
                            styledAttrs.getDrawable(R.styleable.BoxedSeekbar_minImage) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, minImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
                maxImage =
                    requireNotNull(
                            styledAttrs.getDrawable(R.styleable.BoxedSeekbar_maxImage) as?
                                BitmapDrawable
                        ) {
                            "When images are enabled, maxImage can not be null. Please assign a drawable in the layout XML file"
                        }
                        .bitmap
            }

            progressColor =
                styledAttrs.getColor(R.styleable.BoxedSeekbar_progressColor, progressColor)
            backgroundColor =
                styledAttrs.getColor(R.styleable.BoxedSeekbar_backgroundColor, backgroundColor)
            textSizeSp = styledAttrs.getDimension(R.styleable.BoxedSeekbar_textSize, textSizeSp)
            textColor = styledAttrs.getColor(R.styleable.BoxedSeekbar_textColor, textColor)
            enabled = styledAttrs.getBoolean(R.styleable.BoxedSeekbar_enabled, enabled)
            touchDisabled =
                styledAttrs.getBoolean(R.styleable.BoxedSeekbar_touchDisabled, touchDisabled)
            textEnabled = styledAttrs.getBoolean(R.styleable.BoxedSeekbar_textEnabled, textEnabled)
            isSnapEnabled =
                styledAttrs.getBoolean(R.styleable.BoxedSeekbar_snapEnabled, isSnapEnabled)
            currentValue = defaultValue
            styledAttrs.recycle()
        }

        // range check
        currentValue = min(currentValue, max)
        currentValue = max(currentValue, min)

        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true
        progressPaint.style = Paint.Style.STROKE

        textPaint.color = textColor
        textPaint.isAntiAlias = true
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = textSizeSp

        seekbarPaint.alpha = 255
        seekbarPaint.color = backgroundColor
        seekbarPaint.isAntiAlias = true

        scrHeight = context.resources.displayMetrics.heightPixels
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        scrWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        scrHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        progressPaint.strokeWidth = thickness.toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.translate(0f, 0f)
        val path = Path()
        path.addRoundRect(
            if (orientation == Orientation.VERTICAL) {
                RectF(0f, 0f, thickness.toFloat(), length.toFloat())
            } else {
                RectF(0f, 0f, length.toFloat(), thickness.toFloat())
            },
            cornerRadius.toFloat(),
            cornerRadius.toFloat(),
            Path.Direction.CCW
        )
        canvas.clipPath(path, Region.Op.INTERSECT)
        if (orientation == Orientation.VERTICAL) {
            canvas.drawRect(0f, 0f, thickness.toFloat(), length.toFloat(), seekbarPaint)
            canvas.drawLine(
                width.toFloat() / 2,
                height.toFloat(),
                width.toFloat() / 2,
                progressSweep,
                progressPaint
            )
        } else {
            canvas.drawRect(0f, 0f, length.toFloat(), thickness.toFloat(), seekbarPaint)
            canvas.drawLine(
                0f,
                height.toFloat() / 2,
                progressSweep,
                height.toFloat() / 2,
                progressPaint
            )
        }

        if (isImageEnabled) {
            when (currentValue) {
                max -> drawIcon(requireNotNull(maxImage), canvas)
                min -> drawIcon(requireNotNull(minImage), canvas)
                else -> drawIcon(requireNotNull(defaultImage), canvas)
            }
        } else if (textEnabled) {
            drawText(canvas, textPaint, pointsText)
        }

        if (firstRun) {
            firstRun = false
            value = currentValue
        }
    }

    private fun drawText(canvas: Canvas, paint: Paint, text: String) {
        canvas.getClipBounds(canvasClipBoundsRect)
        val canvasWidth = canvasClipBoundsRect.width()
        paint.textAlign = Paint.Align.LEFT
        paint.getTextBounds(text, 0, text.length, canvasClipBoundsRect)
        val x =
            if (orientation == Orientation.VERTICAL) {
                canvasWidth / 2f - canvasClipBoundsRect.width() / 2f - canvasClipBoundsRect.left
            } else {
                textBottomPaddingPx.toFloat()
            }
        canvas.drawText(
            text,
            x,
            if (orientation == Orientation.VERTICAL) {
                (canvas.height - textBottomPaddingPx).toFloat()
            } else {
                (canvas.height / 2 + textBottomPaddingPx / 2).toFloat()
            },
            paint
        )
    }

    private fun drawIcon(bitmap: Bitmap, canvas: Canvas) {
        val newDimension =
            if (orientation == Orientation.VERTICAL) canvas.width / 2 else canvas.height / 2
        val resizedBitmap = getResizedBitmap(bitmap, newDimension, newDimension)
        canvas.drawBitmap(
            resizedBitmap,
            null,
            RectF(
                if (orientation == Orientation.VERTICAL) {
                    canvas.width.toFloat() / 2 - resizedBitmap.width.toFloat() / 2
                } else {
                    resizedBitmap.width.toFloat() / 2
                },
                (canvas.height - resizedBitmap.height).toFloat(),
                if (orientation == Orientation.VERTICAL) {
                    canvas.width.toFloat() / 3 + resizedBitmap.width
                } else {
                    resizedBitmap.width.toFloat() * 3 / 2
                },
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
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabled) return false
        parent.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onValueChangeListener?.onStartTrackingTouch(this)
                if (!touchDisabled) updateOnTouch(event)
            }
            MotionEvent.ACTION_MOVE -> updateOnTouch(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onValueChangeListener?.onStopTrackingTouch(this)
                if (isSnapEnabled) value = roundedProgress
                isPressed = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun updateOnTouch(event: MotionEvent) {
        isPressed = true
        val touchPoint =
            convertTouchEventPoint(if (orientation == Orientation.VERTICAL) event.y else event.x)
        updateProgress(touchPoint.roundToInt())
    }

    private fun convertTouchEventPoint(position: Float): Double =
        when {
            position > length * 2 -> (length * 2).toFloat()
            position < 0 -> 0f
            else -> position
        }.toDouble()

    private fun updateProgress(progress: Int) {
        progressSweep = progress.toFloat()
        val coalescedProgress = max(min(progress, length), 0)

        // convert progress to min-max range
        currentValue = coalescedProgress * (max - min) / length + min
        // reverse value because progress is descending
        if (orientation == Orientation.VERTICAL) currentValue = max + min - currentValue
        // if value is not max or min, apply step
        if (currentValue != max && currentValue != min) {
            currentValue =
                if (orientation == Orientation.VERTICAL) {
                    currentValue - currentValue % step + min % step
                } else {
                    currentValue + currentValue % step + min % step
                }
        }
        onValueChangeListener?.onValueChanged(this, currentValue)
        pointsText = roundedProgress.toString()
        invalidate()
    }

    private fun updateProgressByValue(value: Int) {
        currentValue = value
        currentValue = min(currentValue, max)
        currentValue = max(currentValue, min)
        // convert min-max range to progress
        progressSweep = ((currentValue - min) * length / (max - min)).toFloat()
        // reverse value because progress is descending
        if (orientation == Orientation.VERTICAL) progressSweep = length - progressSweep
        onValueChangeListener?.onValueChanged(this, currentValue)
        pointsText = value.toString()
        invalidate()
    }

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    interface OnValueChangeListener {
        fun onValueChanged(seekbar: BoxedSeekbar, value: Int) = Unit
        fun onStartTrackingTouch(seekbar: BoxedSeekbar) = Unit
        fun onStopTrackingTouch(seekbar: BoxedSeekbar) = Unit
    }

    private enum class Orientation {
        VERTICAL,
        HORIZONTAL
    }
}
