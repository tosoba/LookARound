package com.lookaround.core.android.appunta.renderer.impl

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.PointRenderer

/***
 * This class is used to generate a PointRenderer using a drawable
 * resource
 */
class AugmentedDrawablePointRenderer : PointRenderer {
    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    override fun drawPoint(point: Point, canvas: Canvas, orientation: Orientation?) {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }
        val size = ((10 - point.distance) * 6).toFloat()
        canvas.drawCircle(point.x, point.y, size, circlePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = Typeface.SANS_SERIF
            color = Color.WHITE
        }
        val textWidth = textPaint.breakText(point.name, true, 500f, null).toFloat() / 2
        val blackLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(point.name, point.x - textWidth + 2, point.y + size + 16, blackLinePaint)
        canvas.drawText(point.name, point.x - textWidth, point.y + size + 14, textPaint)
    }
}