package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.*
import android.location.Location
import android.util.AttributeSet
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import kotlin.math.cos
import kotlin.math.sin

class ARRadarView : ARView<RadarMarkerRenderer> {
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val size = measuredWidth.coerceAtLeast(measuredHeight)
        center = size.toFloat() / 2
        setMeasuredDimension(size, size)
    }

    override fun calculateMarkerScreenPosition(marker: ARMarker, location: Location) {
        val markerAngle = getAngleBetween(marker, location) + compassAngle
        val pixelDistance = marker.distance * center / maxDistance
        val markerY = center - pixelDistance * sin(markerAngle)
        val markerX = center + pixelDistance * cos(markerAngle)
        marker.x = markerX.toFloat()
        marker.y = markerY.toFloat()
    }

    override fun preRender(canvas: Canvas, location: Location) {
        drawBackground(canvas)
        compassAngle = orientation.y.toDouble()
    }

    override fun shouldDraw(marker: ARMarker): Boolean = marker.distance < maxDistance

    override fun postRender(canvas: Canvas, location: Location) {
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
