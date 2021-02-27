package com.lookaround.core.android.ar.renderer.impl

import android.content.Context
import android.content.res.Configuration
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
import com.lookaround.core.android.ext.actionBarHeight
import com.lookaround.core.android.ext.statusBarHeight
import java.util.*
import kotlin.math.abs

class CameraMarkerRenderer(
    context: Context,
    var location: Location = Location(""),
) : MarkerRenderer {
    private val markerHeight: Float
    private val markerWidth: Float
    private val statusBarHeight: Float = context.statusBarHeight.toFloat()
    private val actionBarHeight: Float = context.actionBarHeight

    init {
        val displayMetrics = context.resources.displayMetrics
        val orientation = context.resources.configuration.orientation

        val numberOfRows =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                NUMBER_OF_ROWS_PORTRAIT
            } else {
                NUMBER_OF_ROWS_LANDSCAPE
            }
        markerHeight =
            (displayMetrics.heightPixels - statusBarHeight - actionBarHeight) / numberOfRows -
                MARKER_VERTICAL_SPACING

        val markerWidthDivisor =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                MARKER_WIDTH_DIVISOR_PORTRAIT
            } else {
                MARKER_WIDTH_DIVISOR_LANDSCAPE
            }
        markerWidth = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
    }

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
                marker.x - markerWidth / 2,
                marker.y - markerHeight / 2,
                marker.x + markerWidth / 2,
                marker.y + markerHeight / 2)
        val width = (rect.width() - 10).toInt() // 10 to keep some space on the right for the "..."
        val text =
            TextUtils.ellipsize(
                "The loooooong text", textPaint, width.toFloat(), TextUtils.TruncateAt.END)
        canvas.drawText(
            text, 0, text.length, marker.x - markerWidth / 2 + TEXT_OFFSET, marker.y, textPaint)
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

        var bestY = statusBarHeight + actionBarHeight
        while (taken.contains(bestY)) bestY += (markerHeight + MARKER_VERTICAL_SPACING)
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
        private const val TEXT_OFFSET = 20f
        private const val TAKEN_BEARING_LIMIT = 45.0f
        private const val MARKER_VERTICAL_SPACING = 50.0f
        private const val NUMBER_OF_ROWS_PORTRAIT = 4
        private const val NUMBER_OF_ROWS_LANDSCAPE = 2
        private const val MARKER_WIDTH_DIVISOR_PORTRAIT = 2
        private const val MARKER_WIDTH_DIVISOR_LANDSCAPE = 4
    }
}
