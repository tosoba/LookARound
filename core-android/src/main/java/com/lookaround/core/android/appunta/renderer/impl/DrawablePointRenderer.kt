package com.lookaround.core.android.appunta.renderer.impl

import android.content.res.Resources
import android.graphics.*
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.PointRenderer

/***
 * This class is used to generate a PointRenderer using a drawable resource
 */
class DrawablePointRenderer(private val res: Resources, private val id: Int) : PointRenderer {
    private val bitmap: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        BitmapFactory.decodeResource(res, id)
    }

    private val textPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.LEFT
            textSize = 20f
            typeface = Typeface.SANS_SERIF
            color = Color.WHITE
        }
    }

    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    override fun drawPoint(point: Point, canvas: Canvas, orientation: Orientation) {
        val xOff = bitmap.width / 2
        val yOff = bitmap.height / 2
        canvas.drawBitmap(bitmap, point.x - xOff, point.y - yOff, null)
        canvas.drawText(point.name, point.x + xOff, point.y + 8, textPaint)
    }
}