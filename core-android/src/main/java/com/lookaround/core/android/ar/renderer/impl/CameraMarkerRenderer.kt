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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

class CameraMarkerRenderer(context: Context) : MarkerRenderer {
    private val markerHeight: Float
    private val markerWidth: Float
    private val statusBarHeight: Float = context.statusBarHeight.toFloat()
    private val actionBarHeight: Float = context.actionBarHeight
    private val screenHeight: Float

    init {
        val displayMetrics = context.resources.displayMetrics
        screenHeight = displayMetrics.heightPixels.toFloat()
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

    private val maxPageStateFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val maxPageFlow: Flow<Int>
        get() = maxPageStateFlow
    private var maxPage: Int = 0

    var currentPage: Int = 0
        @MainThread
        set(value) {
            assert(value >= 0)
            field = value
        }
    var povLocation: Location? = null
        @MainThread
        internal set(value) {
            field = value
            markerBearingsMap.clear()
            markersMap.values.forEach { marker ->
                calculateBearingBetween(requireNotNull(value), marker)
            }
        }

    private val markersMap: LinkedHashMap<UUID, CameraMarker> = LinkedHashMap()
    private val markerBearingsMap: TreeMap<Float, CameraMarker> = TreeMap()

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
        marker.y = wrapped.pagedPosition?.y ?: wrapped.calculatePagedPosition().y
        wrapped.pagedPosition?.page?.let { if (it > maxPage) maxPage = it }
        if (wrapped.pagedPosition?.page != currentPage) return

        val rect =
            RectF(
                marker.x - markerWidth / 2,
                marker.y - markerHeight / 2,
                marker.x + markerWidth / 2,
                marker.y + markerHeight / 2
            )
        val width = (rect.width() - 10).toInt() // 10 to keep some space on the right for the "..."
        val text =
            TextUtils.ellipsize(
                marker.wrapped.name,
                textPaint,
                width.toFloat(),
                TextUtils.TruncateAt.END
            )
        canvas.drawText(
            text,
            0,
            text.length,
            marker.x - markerWidth / 2 + TEXT_OFFSET,
            marker.y,
            textPaint
        )
        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)
    }

    override fun postDrawAll() {
        maxPageStateFlow.value = maxPage
        if (maxPage > currentPage) currentPage = maxPage
        maxPage = 0
    }

    private fun CameraMarker.calculatePagedPosition(): PagedPosition {
        val bearingThis = povLocationBearing ?: throw IllegalStateException()
        val takenPositions =
            markerBearingsMap
                .subMap(bearingThis - TAKEN_BEARING_LIMIT, bearingThis + TAKEN_BEARING_LIMIT)
                .map { (_, marker) -> marker.pagedPosition }
                .filterNotNull()
                .toSet()
        val baseY = statusBarHeight + actionBarHeight
        val position = PagedPosition(baseY, 0)
        while (takenPositions.contains(position)) {
            position.y += markerHeight + MARKER_VERTICAL_SPACING
            if (position.y >= screenHeight) {
                position.y = baseY
                ++position.page
            }
        }
        pagedPosition = position
        return position
    }

    @MainThread
    operator fun plusAssign(marker: ARMarker) {
        if (markersMap.contains(marker.wrapped.id)) return
        val cameraMarker = CameraMarker(marker)
        calculateBearingBetweenLocationAnd(cameraMarker)
        markersMap[marker.wrapped.id] = cameraMarker
    }

    @MainThread
    operator fun plusAssign(markers: Collection<ARMarker>) {
        markers.forEach { marker ->
            if (markersMap.contains(marker.wrapped.id)) return@forEach
            val cameraMarker = CameraMarker(marker)
            calculateBearingBetweenLocationAnd(cameraMarker)
            markersMap[marker.wrapped.id] = cameraMarker
        }
    }

    private fun calculateBearingBetweenLocationAnd(marker: CameraMarker) {
        povLocation?.let { calculateBearingBetween(it, marker) }
    }

    private fun calculateBearingBetween(userLocation: Location, marker: CameraMarker) {
        val bearing = userLocation.bearingTo(marker.wrapped.wrapped.location)
        marker.povLocationBearing = bearing
        markerBearingsMap[bearing] = marker
    }

    @MainThread
    operator fun minusAssign(marker: ARMarker) {
        markersMap.remove(marker.wrapped.id)?.let { removed ->
            removed.povLocationBearing?.let(markerBearingsMap::remove)
            resetPaging()
        }
    }

    @MainThread
    operator fun minusAssign(markers: Collection<ARMarker>) {
        var removedAny = false
        markers.forEach { marker ->
            markersMap.remove(marker.wrapped.id)?.let { removed ->
                removed.povLocationBearing?.let(markerBearingsMap::remove)
                removedAny = true
            }
        }
        if (removedAny) resetPaging()
    }

    private fun resetPaging() {
        markersMap.values.forEach { it.pagedPosition = null }
    }

    private class CameraMarker(
        val wrapped: ARMarker,
        var pagedPosition: PagedPosition? = null,
        var povLocationBearing: Float? = null
    ) {
        override fun equals(other: Any?): Boolean =
            this === other || (other is CameraMarker && other.wrapped == wrapped)

        override fun hashCode(): Int = Objects.hash(wrapped)
    }

    private data class PagedPosition(var y: Float, var page: Int)

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
