package com.lookaround.core.android.appunta.renderer.impl

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import com.lookaround.core.android.appunta.marker.CameraMarker
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.renderer.MarkerRenderer
import java.util.*
import kotlin.math.abs

class NoOverlapRenderer(
    var userLocation: Location = Location(""),
    private val dialogHeight: Float = 100f,
    private val dialogWidth: Float = 400f
) : MarkerRenderer {
    private val markers: MutableMap<UUID, WrappedPoint> = mutableMapOf()

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

    override fun drawPoint(cameraMarker: CameraMarker, canvas: Canvas, orientation: Orientation) {
        val wrapped = markers[cameraMarker.marker.id] ?: return
        cameraMarker.y =
            wrapped.screenY
                ?: run {
                    val calculated = wrapped.calculateScreenY()
                    wrapped.screenY = calculated
                    calculated
                }

        val rect =
            RectF(
                cameraMarker.x - dialogWidth / 2,
                cameraMarker.y - dialogHeight / 2,
                cameraMarker.x + dialogWidth / 2,
                cameraMarker.y + dialogHeight / 2)
        val width = (rect.width() - 10).toInt() // 10 to keep some space on the right for the "..."
        val text =
            TextUtils.ellipsize(
                "The loooooong text", textPaint, width.toFloat(), TextUtils.TruncateAt.END)
        canvas.drawText(
            text,
            0,
            text.length,
            cameraMarker.x - dialogWidth / 2 + TEXT_OFFSET,
            cameraMarker.y,
            textPaint)
        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)
    }

    private class WrappedPoint(val wrapped: CameraMarker, var screenY: Float? = null)

    private fun WrappedPoint.calculateScreenY(): Float {
        val taken = mutableSetOf<Float>()
        val bearingThis = userLocation.bearingTo(wrapped.marker.location)
        markers.values.forEach { marker ->
            if (marker == this) return@forEach
            marker.screenY?.let { screenY ->
                val bearingCurrent = userLocation.bearingTo(marker.wrapped.marker.location)
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
    operator fun plusAssign(cameraMarker: CameraMarker) {
        if (markers.contains(cameraMarker.marker.id)) return
        markers[cameraMarker.marker.id] = WrappedPoint(cameraMarker)
    }

    @MainThread
    operator fun plusAssign(cameraMarkers: Collection<CameraMarker>) {
        cameraMarkers.forEach { marker ->
            if (markers.contains(marker.marker.id)) return@forEach
            markers[marker.marker.id] = WrappedPoint(marker)
        }
    }

    companion object {
        private const val BASE_SCREEN_Y = 100f
        private const val TEXT_OFFSET = 20f
        private const val TAKEN_BEARING_LIMIT = 45.0
    }
}
