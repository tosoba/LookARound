package com.lookaround.core.android.appunta.renderer.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.PointRenderer

/***
 * A simple Point renderer used as default by the compass
 */
class SimplePointRenderer : PointRenderer {
    override fun drawPoint(point: Point, canvas: Canvas, orientation: Orientation?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x44FFFFFF
        canvas.drawCircle(point.x, point.y, 5f, paint)
        paint.color = 0x33FFFFFF
        canvas.drawCircle(point.x, point.y, 4f, paint)
        paint.color = 0x66FFFFFF
        canvas.drawCircle(point.x, point.y, 3f, paint)
        paint.color = -0x66000001
        canvas.drawCircle(point.x, point.y, 2f, paint)
        paint.color = -0x1
        canvas.drawCircle(point.x, point.y, 1f, paint)
    }
}