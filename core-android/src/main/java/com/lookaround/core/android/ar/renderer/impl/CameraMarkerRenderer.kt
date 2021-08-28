package com.lookaround.core.android.ar.renderer.impl

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import com.lookaround.core.android.ext.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

class CameraMarkerRenderer(context: Context) : MarkerRenderer {
    override val markerHeightPx: Float
    override val markerWidthPx: Float

    private val statusBarHeightPx: Float = context.statusBarHeight.toFloat()
    private val actionBarHeightPx: Float = context.actionBarHeight
    private val cameraViewHeightPx: Float

    private val markerCornerRadiusPx: Float = context.dpToPx(MARKER_CORNER_RADIUS_DP)
    private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
    private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
    private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

    init {
        val displayMetrics = context.resources.displayMetrics
        val bottomNavigationViewHeight = context.bottomNavigationViewHeight
        cameraViewHeightPx = displayMetrics.heightPixels.toFloat() - bottomNavigationViewHeight

        val orientation = context.resources.configuration.orientation

        val numberOfRows =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                NUMBER_OF_ROWS_PORTRAIT
            } else {
                NUMBER_OF_ROWS_LANDSCAPE
            }
        val cameraViewHeight =
            displayMetrics.heightPixels -
                statusBarHeightPx -
                actionBarHeightPx -
                bottomNavigationViewHeight
        markerHeightPx = cameraViewHeight / numberOfRows - MARKER_VERTICAL_SPACING_PX

        val markerWidthDivisor =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                MARKER_WIDTH_DIVISOR_PORTRAIT
            } else {
                MARKER_WIDTH_DIVISOR_LANDSCAPE
            }
        markerWidthPx = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
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

    private val backgroundPaint: Paint by
        lazy(LazyThreadSafetyMode.NONE) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL_AND_STROKE
                color = Color.parseColor("#d9ffffff")
                alpha = 75
            }
        }

    private val titleTextPaint: TextPaint by
        lazy(LazyThreadSafetyMode.NONE) {
            TextPaint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
                textSize = markerTitleTextSizePx
                textAlign = Paint.Align.LEFT
                isLinearText = true
            }
        }

    private val distanceTextPaint: TextPaint by
        lazy(LazyThreadSafetyMode.NONE) {
            TextPaint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
                textSize = markerDistanceTextSizePx
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
                marker.x - markerWidthPx / 2,
                marker.y - markerHeightPx / 2,
                marker.x + markerWidthPx / 2,
                marker.y + markerHeightPx / 2
            )
        canvas.drawRoundRect(rect, markerCornerRadiusPx, markerCornerRadiusPx, backgroundPaint)
        canvas.drawTitleText(marker, rect)
        canvas.drawDistanceText(marker, rect)
    }

    private fun Canvas.drawTitleText(marker: ARMarker, rect: RectF) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawMultilineText(
                text = marker.wrapped.name,
                textPaint = titleTextPaint,
                width = (rect.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX).toInt(),
                x = marker.x - markerWidthPx / 2 + markerPaddingPx,
                y = marker.y - markerHeightPx / 2 + markerPaddingPx,
                ellipsize = TextUtils.TruncateAt.END,
                maxLines = 2,
            )
        } else {
            val title =
                TextUtils.ellipsize(
                    marker.wrapped.name,
                    titleTextPaint,
                    rect.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX,
                    TextUtils.TruncateAt.END
                )
            drawText(
                title,
                0,
                title.length,
                marker.x - markerWidthPx / 2 + markerPaddingPx,
                marker.y - markerHeightPx / 2 + markerPaddingPx + markerTitleTextSizePx,
                titleTextPaint
            )
        }
    }

    private fun Canvas.drawDistanceText(marker: ARMarker, rect: RectF) {
        val distance =
            TextUtils.ellipsize(
                marker.distance.formattedDistance,
                distanceTextPaint,
                rect.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX,
                TextUtils.TruncateAt.END
            )
        drawText(
            distance,
            0,
            distance.length,
            marker.x - markerWidthPx / 2 + markerPaddingPx,
            marker.y + markerHeightPx / 2 - markerPaddingPx,
            distanceTextPaint
        )
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
                .subMap(
                    bearingThis - TAKEN_BEARING_LIMIT_DEGREES,
                    bearingThis + TAKEN_BEARING_LIMIT_DEGREES
                )
                .map { (_, marker) -> marker.pagedPosition }
                .filterNotNull()
                .toSet()
        val baseY = statusBarHeightPx + actionBarHeightPx
        val position = PagedPosition(baseY, 0)
        while (takenPositions.contains(position)) {
            position.y += markerHeightPx + MARKER_VERTICAL_SPACING_PX
            if (position.y >= cameraViewHeightPx) {
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
        private const val TAKEN_BEARING_LIMIT_DEGREES = 45f
        private const val MARKER_VERTICAL_SPACING_PX = 50f
        private const val MARKER_CORNER_RADIUS_DP = 12f
        private const val NUMBER_OF_ROWS_PORTRAIT = 4
        private const val NUMBER_OF_ROWS_LANDSCAPE = 2
        private const val MARKER_WIDTH_DIVISOR_PORTRAIT = 2
        private const val MARKER_WIDTH_DIVISOR_LANDSCAPE = 4
        private const val MARKER_PADDING_DP = 10f
        private const val ELLIPSIS_WIDTH_PX = 10f
        private const val MARKER_TITLE_TEXT_SIZE_SP = 20f
        private const val MARKER_DISTANCE_TEXT_SIZE_SP = 16f
    }
}
