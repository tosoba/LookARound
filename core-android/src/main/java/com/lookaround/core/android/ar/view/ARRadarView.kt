package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.*
import android.location.Location
import android.util.AttributeSet
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class ARRadarView : ARView<RadarMarkerRenderer> {
    var disabled: Boolean = false
    var rotableBackground: Int = -1
        set(value) {
            field = value
            rotableBackgroundBitmap = BitmapFactory.decodeResource(resources, value)
        }
    private var center: Float = 0f
    private var rotableBackgroundBitmap: Bitmap? = null
    private var compassAngle: Double = 0.0

    private val centerPaint: Paint by
        lazy(LazyThreadSafetyMode.NONE) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -0xff5f2e } }

    override val ARMarker.shouldBeDrawn: Boolean
        get() = distance < maxRange

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
        val pixelDistance = marker.distance * center / maxRange
        val markerY = center - pixelDistance * sin(markerAngle)
        val markerX = center + pixelDistance * cos(markerAngle)
        marker.x = markerX.toFloat()
        marker.y = markerY.toFloat()
    }

    override fun preDraw(canvas: Canvas, location: Location) {
        if (disabled) return
        drawBackground(canvas)
        compassAngle = orientation.azimuth.toDouble()
    }

    override fun postDraw(canvas: Canvas, location: Location) {
        if (disabled) return
        canvas.drawCircle(center, center, 5f, centerPaint)
    }

    private fun getAngleBetween(marker: ARMarker, location: Location): Double =
        atan2(
            marker.wrapped.location.latitude - location.latitude,
            marker.wrapped.location.longitude - location.longitude
        )

    private fun drawBackground(canvas: Canvas) {
        rotableBackgroundBitmap?.let { bitmap ->
            val transform = Matrix()
            transform.setRectToRect(
                RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
                RectF(0f, 0f, width.toFloat(), width.toFloat()),
                Matrix.ScaleToFit.CENTER
            )
            transform.preRotate(
                (-Math.toDegrees(compassAngle)).toFloat(),
                bitmap.width.toFloat() / 2,
                bitmap.height.toFloat() / 2
            )
            canvas.drawBitmap(bitmap, transform, null)
        }
    }
}
