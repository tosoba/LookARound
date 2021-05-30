package com.lookaround.core.android.ar.renderer.impl

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import com.lookaround.core.android.ext.actionBarHeight
import com.lookaround.core.android.ext.bottomNavigationViewHeight
import com.lookaround.core.android.ext.dpToPx
import com.lookaround.core.android.ext.statusBarHeight
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class CameraMarkerRenderer(context: Context) : MarkerRenderer {
    override val markerHeight: Float
    override val markerWidth: Float
    private val statusBarHeight: Float = context.statusBarHeight.toFloat()
    private val actionBarHeight: Float = context.actionBarHeight
    private val screenHeight: Float
    private val markerCornerRadius: Float

    init {
        val displayMetrics = context.resources.displayMetrics
        val bottomNavigationViewHeight = context.bottomNavigationViewHeight
        screenHeight = displayMetrics.heightPixels.toFloat() - bottomNavigationViewHeight
        val orientation = context.resources.configuration.orientation

        val numberOfRows =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                NUMBER_OF_ROWS_PORTRAIT
            } else {
                NUMBER_OF_ROWS_LANDSCAPE
            }
        val cameraViewHeight =
            displayMetrics.heightPixels -
                statusBarHeight -
                actionBarHeight -
                bottomNavigationViewHeight
        markerHeight = cameraViewHeight / numberOfRows - MARKER_VERTICAL_SPACING

        val markerWidthDivisor =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                MARKER_WIDTH_DIVISOR_PORTRAIT
            } else {
                MARKER_WIDTH_DIVISOR_LANDSCAPE
            }
        markerWidth = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
        markerCornerRadius = context.dpToPx(12f)
    }

    private val maxPageStateFlow: MutableStateFlow<MaxPageChanged> =
        MutableStateFlow(MaxPageChanged(0, false))
    val maxPageFlow: Flow<MaxPageChanged>
        get() = maxPageStateFlow
    var maxPage: Int = 0
        private set

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
        marker.y = wrapped.pagedPosition?.y ?: calculatePagedPositionOf(wrapped).y
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
        canvas.drawRoundRect(rect, markerCornerRadius, markerCornerRadius, backgroundPaint)
    }

    override fun postDrawAll() {
        val changeCurrent = currentPage > maxPage
        maxPageStateFlow.value = MaxPageChanged(maxPage, changeCurrent)
        if (changeCurrent) currentPage = maxPage
        maxPage = 0
    }

    override fun onSaveInstanceState(): Bundle =
        bundleOf(
            SavedStateKeys.CURRENT_PAGE.name to currentPage,
            SavedStateKeys.MAX_PAGE.name to maxPage
        )

    override fun onRestoreInstanceState(bundle: Bundle?) {
        bundle?.let {
            currentPage = it.getInt(SavedStateKeys.CURRENT_PAGE.name)
            maxPage = it.getInt(SavedStateKeys.MAX_PAGE.name)
        }
    }

    private fun calculatePagedPositionOf(marker: CameraMarker): PagedPosition {
        val bearingThis = marker.povLocationBearing ?: throw IllegalStateException()
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
        marker.pagedPosition = position
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

    internal fun isOnCurrentPage(marker: ARMarker): Boolean =
        markersMap[marker.wrapped.id]?.pagedPosition?.page == currentPage

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

    data class MaxPageChanged(val maxPage: Int, val setCurrentPage: Boolean)

    private enum class SavedStateKeys {
        MAX_PAGE,
        CURRENT_PAGE
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
