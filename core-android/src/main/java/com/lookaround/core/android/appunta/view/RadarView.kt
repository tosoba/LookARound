package com.lookaround.core.android.appunta.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.lookaround.core.android.appunta.point.Point
import kotlin.math.cos
import kotlin.math.sin

class RadarView : AppuntaView {
    var rotableBackground = 0
        set(value) {
            field = value
            rotableBackgroundBitmap = BitmapFactory.decodeResource(this.resources, value)
        }
    private var center = 0f
    private var rotableBackgroundBitmap: Bitmap? = null
    private var compassAngle = 0.0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    /***
     * Returns the correct size of the control when needed (Basically
     * maintaining the ratio)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val size = measuredWidth.coerceAtLeast(measuredHeight)
        center = size.toFloat() / 2
        setMeasuredDimension(size, size)
    }

    override fun calculatePointCoordinates(point: Point) {
        val pointAngle = getAngle(point) + compassAngle
        val pixelDistance = point.distance * center / maxDistance
        val pointY = center - pixelDistance * sin(pointAngle)
        val pointX = center + pixelDistance * cos(pointAngle)
        point.x = pointX.toFloat()
        point.y = pointY.toFloat()
    }

    override fun preRender(canvas: Canvas) {
        drawBackground(canvas)
        compassAngle = orientation.y.toDouble()
    }

    override fun postRender(canvas: Canvas) {
        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pointPaint.color = -0xff5f2e
        canvas.drawCircle(center, center, 5f, pointPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        rotableBackgroundBitmap?.let { bitmap ->
            val transform = Matrix()
            transform.setRectToRect(
                RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                RectF(0f, 0f, width.toFloat(), width.toFloat()), Matrix.ScaleToFit.CENTER
            )
            transform.preRotate(
                (-Math.toDegrees(compassAngle)).toFloat(),
                bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2
            )
            canvas.drawBitmap(bitmap, transform, null)
        }
    }
}