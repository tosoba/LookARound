package com.lookaround.core.android.ar.renderer.impl

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import java.util.*
import kotlin.math.abs

class CameraMarkerRenderer(
    var location: Location = Location(""),
    private val dialogHeight: Float = 100f,
    private val dialogWidth: Float = 400f
) : MarkerRenderer {
    private val markersMap: MutableMap<UUID, CameraMarker> = mutableMapOf()

    private val backgroundPaint: Paint by lazy(LazyThreadSafetyMode.NONE) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            color = Color.parseColor("#70ffffff")
        }
    }

    private val textPaint: TextPaint by lazy(LazyThreadSafetyMode.NONE) {
        TextPaint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
            textSize = 40f
            textAlign = Paint.Align.LEFT
            isLinearText = true
        }
    }

    override fun draw(marker: ARMarker, canvas: Canvas, orientation: Orientation) {
        val wrapped = markersMap[marker.wrapped.id] ?: return
        marker.y =
            wrapped.screenY
                ?: run {
                    val calculated = wrapped.calculateScreenY()
                    wrapped.screenY = calculated
                    calculated
                }

        val rect =
            RectF(
                marker.x - dialogWidth / 2,
                marker.y - dialogHeight / 2,
                marker.x + dialogWidth / 2,
                marker.y + dialogHeight / 2)
        val width = (rect.width() - 10).toInt() // 10 to keep some space on the right for the "..."
        val text =
            TextUtils.ellipsize(
                "The loooooong text", textPaint, width.toFloat(), TextUtils.TruncateAt.END)
        canvas.drawText(
            text, 0, text.length, marker.x - dialogWidth / 2 + TEXT_OFFSET, marker.y, textPaint)
        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)
    }

    private class CameraMarker(val wrapped: ARMarker, var screenY: Float? = null)

    private fun CameraMarker.calculateScreenY(): Float {
        val taken = mutableSetOf<Float>()
        val bearingThis = location.bearingTo(wrapped.wrapped.location)
        markersMap.values.forEach { marker ->
            if (marker == this) return@forEach
            marker.screenY?.let { screenY ->
                val bearingCurrent = location.bearingTo(marker.wrapped.wrapped.location)
                if (abs(bearingCurrent - bearingThis) < TAKEN_BEARING_LIMIT &&
                    !taken.contains(screenY)) {
                    taken.add(screenY)
                }
            }
        }

        var bestY = BASE_SCREEN_Y
        while (taken.contains(bestY)) bestY += dialogHeight
        return bestY
    }

    @MainThread
    operator fun plusAssign(marker: ARMarker) {
        if (markersMap.contains(marker.wrapped.id)) return
        markersMap[marker.wrapped.id] = CameraMarker(marker)
    }

    @MainThread
    operator fun plusAssign(markers: Collection<ARMarker>) {
        markers.forEach { marker ->
            if (markersMap.contains(marker.wrapped.id)) return@forEach
            markersMap[marker.wrapped.id] = CameraMarker(marker)
        }
    }

    companion object {
        private const val BASE_SCREEN_Y = 100f
        private const val TEXT_OFFSET = 20f
        private const val TAKEN_BEARING_LIMIT = 45.0
    }
}
