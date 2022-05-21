package com.lookaround.core.android.ar.renderer.impl

import android.graphics.Canvas
import android.graphics.Paint
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer

class RadarMarkerRenderer : MarkerRenderer {
    var disabled: Boolean = false
    var enlarged: Boolean = false

    override fun draw(markers: List<ARMarker>, canvas: Canvas, orientation: Orientation) {
        if (disabled) return
        markers.forEach { marker ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0x44000000
            canvas.drawCircle(marker.x, marker.y, if (enlarged) 10f else 5f, paint)
            paint.color = 0x33000000
            canvas.drawCircle(marker.x, marker.y, if (enlarged) 8f else 4f, paint)
            paint.color = 0x66000000
            canvas.drawCircle(marker.x, marker.y, if (enlarged) 6f else 3f, paint)
            paint.color = -0x66000001
            canvas.drawCircle(marker.x, marker.y, if (enlarged) 4f else 2f, paint)
            paint.color = -0x1
            canvas.drawCircle(marker.x, marker.y, if (enlarged) 2f else 1f, paint)
        }
    }
}
