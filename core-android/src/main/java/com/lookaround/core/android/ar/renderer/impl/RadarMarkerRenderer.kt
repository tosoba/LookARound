package com.lookaround.core.android.ar.renderer.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer

class RadarMarkerRenderer : MarkerRenderer {
    var disabled: Boolean = false

    override fun draw(markers: List<ARMarker>, canvas: Canvas, orientation: Orientation) {
        if (disabled) return
        markers.forEach { marker ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0x44000000
            canvas.drawCircle(marker.x, marker.y, 5f, paint)
            paint.color = 0x33000000
            canvas.drawCircle(marker.x, marker.y, 4f, paint)
            paint.color = 0x66000000
            canvas.drawCircle(marker.x, marker.y, 3f, paint)
            paint.color = -0x66000001
            canvas.drawCircle(marker.x, marker.y, 2f, paint)
            paint.color = -0x1
            canvas.drawCircle(marker.x, marker.y, 1f, paint)
        }
    }
}
