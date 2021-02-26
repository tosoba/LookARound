package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.lookaround.core.android.ar.marker.ARMarker
import kotlin.math.cos
import kotlin.math.sin

class ARRadarView : ARView {
    var rotableBackground: Int = 0
        set(value) {
            field = value
            rotableBackgroundBitmap = BitmapFactory.decodeResource(resources, value)
        }
    private var center: Float = 0f
    private var rotableBackgroundBitmap: Bitmap? = null
    private var compassAngle: Double = 0.0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    /** * Returns the correct size of the control when needed (Basically maintaining the ratio) */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val size = measuredWidth.coerceAtLeast(measuredHeight)
        center = size.toFloat() / 2
        setMeasuredDimension(size, size)
    }

    override fun calculateMarkerCoordinates(marker: ARMarker) {
        val markerAngle = getAngle(marker) + compassAngle
        val pixelDistance = marker.distance * center / maxDistance
        val markerY = center - pixelDistance * sin(markerAngle)
        val markerX = center + pixelDistance * cos(markerAngle)
        marker.x = markerX.toFloat()
        marker.y = markerY.toFloat()
    }

    override fun preRender(canvas: Canvas) {
        drawBackground(canvas)
        compassAngle = orientation.y.toDouble()
    }

    override fun shouldDraw(marker: ARMarker): Boolean =
        marker.distance < maxDistance

    override fun postRender(canvas: Canvas) {
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        markerPaint.color = -0xff5f2e
        canvas.drawCircle(center, center, 5f, markerPaint)
    }

    private fun drawBackground(canvas: Canvas) {
        rotableBackgroundBitmap?.let { bitmap ->
            val transform = Matrix()
            transform.setRectToRect(
                RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                RectF(0f, 0f, width.toFloat(), width.toFloat()),
                Matrix.ScaleToFit.CENTER)
            transform.preRotate(
                (-Math.toDegrees(compassAngle)).toFloat(),
                bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2)
            canvas.drawBitmap(bitmap, transform, null)
        }
    }
}
