package com.lookaround.core.android.ar.renderer.impl

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer

class RadarMarkerRenderer : MarkerRenderer {
    override fun draw(marker: ARMarker, canvas: Canvas, orientation: Orientation): RectF {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x44FFFFFF
        canvas.drawCircle(marker.x, marker.y, 5f, paint)
        paint.color = 0x33FFFFFF
        canvas.drawCircle(marker.x, marker.y, 4f, paint)
        paint.color = 0x66FFFFFF
        canvas.drawCircle(marker.x, marker.y, 3f, paint)
        paint.color = -0x66000001
        canvas.drawCircle(marker.x, marker.y, 2f, paint)
        paint.color = -0x1
        canvas.drawCircle(marker.x, marker.y, 1f, paint)
        return RectF(marker.x - 2.5f, marker.y - 2.5f, marker.x + 2.5f, marker.y + 2.5f)
    }
}
