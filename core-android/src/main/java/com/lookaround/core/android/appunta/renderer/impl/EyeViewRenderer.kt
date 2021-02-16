package com.lookaround.core.android.appunta.renderer.impl

import android.content.res.Resources
import android.graphics.*
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.PointRenderer

/***
 * This class is used to generate a PointRenderer using a drawable
 * resource
 */
class EyeViewRenderer(
    private val res: Resources,
    private val selectedId: Int,
    private val unselectedId: Int
) : PointRenderer {
    private val selectedBitmap: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        BitmapFactory.decodeResource(res, selectedId)
    }

    private val unselectedBitmap: Bitmap by lazy(LazyThreadSafetyMode.NONE) {
        BitmapFactory.decodeResource(res, unselectedId)
    }

    private val textPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            textAlign = Paint.Align.LEFT
            textSize = 20f
            typeface = Typeface.SANS_SERIF
            color = Color.WHITE
        }
    }

    private val blackLinePaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.LEFT
        }
    }

    /***
     * This methods paints the drawable received in constructor and writes the point name beside it
     */
    override fun drawPoint(point: Point, canvas: Canvas, orientation: Orientation?) {
        val xSelectedOff = selectedBitmap.width / 2
        val ySelectedOff = selectedBitmap.height / 2
        val xUnselectedOff = unselectedBitmap.width / 2
        val yUnselectedOff = unselectedBitmap.height / 2
        point.y = 100f
        if (point.isSelected) {
            canvas.drawBitmap(selectedBitmap, point.x - xSelectedOff, point.y - ySelectedOff, null)
        } else {
            canvas.drawBitmap(unselectedBitmap, point.x - xUnselectedOff, point.y - yUnselectedOff, null)
        }
        canvas.rotate(315f, point.x, point.y)
        canvas.drawText(point.name, point.x + 35, point.y, blackLinePaint)
        canvas.drawText(point.name, point.x + 34, point.y - 2, textPaint)
        canvas.rotate(-315f, point.x, point.y)
    }
}