package com.lookaround.core.android.ar.renderer.impl

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import com.lookaround.core.android.ext.*
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class CameraMarkerRenderer(context: Context) : MarkerRenderer {
    private val screenOrientation: Int = context.resources.configuration.orientation

    private val markerPaddingPx: Float = context.dpToPx(MARKER_PADDING_DP)
    private val markerTitleTextSizePx: Float = context.spToPx(MARKER_TITLE_TEXT_SIZE_SP)
    private val markerDistanceTextSizePx: Float = context.spToPx(MARKER_DISTANCE_TEXT_SIZE_SP)

    override val markerHeightPx: Float
    override val markerWidthPx: Float

    private val statusBarHeightPx: Float = context.statusBarHeightPx.toFloat()
    private val actionBarHeightPx: Float = context.actionBarHeightPx

    init {
        val displayMetrics = context.resources.displayMetrics
        val cameraViewHeight =
            displayMetrics.heightPixels -
                statusBarHeightPx -
                actionBarHeightPx -
                context.bottomNavigationViewHeightPx
        markerHeightPx = cameraViewHeight / numberOfRows - MARKER_VERTICAL_SPACING_PX
        val markerWidthDivisor =
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
                MARKER_WIDTH_DIVISOR_PORTRAIT
            } else {
                MARKER_WIDTH_DIVISOR_LANDSCAPE
            }
        markerWidthPx = (displayMetrics.widthPixels / markerWidthDivisor).toFloat()
    }

    var povLocation: Location? = null
        @MainThread set
    var currentPage: Int = 0
        @MainThread
        set(value) {
            assert(value >= 0)
            field = value
        }
    var maxPage: Int = 0
        private set(value) {
            assert(value >= 0)
            field = value
        }

    private var firstFrame: Boolean = true
    private var lastDrawnMarkerIds = HashSet<UUID>()

    private val markersDrawnStateFlow = MutableStateFlow(MarkersDrawn(currentPage, maxPage))
    val markersDrawnFlow: Flow<MarkersDrawn>
        get() = markersDrawnStateFlow

    private val drawnRectsStateFlow = MutableStateFlow<List<RectF>>(emptyList())
    val drawnRectsFlow: Flow<List<RectF>>
        get() = drawnRectsStateFlow

    var disabled: Boolean = false
        @MainThread set

    private val pagedMarkers = HashMap<UUID, PagedMarker>()
    private val pagedMarkerPositions = TreeMap<Float, MutableSet<PagedPosition>>()

    private val numberOfRows: Int
        get() =
            if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) NUMBER_OF_ROWS_PORTRAIT
            else NUMBER_OF_ROWS_LANDSCAPE

    private val titleTextPaint: TextPaint by
        lazy(LazyThreadSafetyMode.NONE) {
            TextPaint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
                textSize = markerTitleTextSizePx
                textAlign = Paint.Align.LEFT
                typeface = Typeface.DEFAULT_BOLD
                isLinearText = true
                setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
            }
        }

    private val distanceTextPaint: TextPaint by
        lazy(LazyThreadSafetyMode.NONE) {
            TextPaint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
                textSize = markerDistanceTextSizePx
                textAlign = Paint.Align.LEFT
                isLinearText = true
                setShadowLayer(2.0f, 3.0f, 3.0f, Color.GRAY)
            }
        }

    private val ARMarker.rectF: RectF
        get() =
            RectF(
                x - markerWidthPx / 2,
                y - markerHeightPx / 2,
                x + markerWidthPx / 2,
                y + markerHeightPx / 2
            )

    override fun draw(markers: List<ARMarker>, canvas: Canvas, orientation: Orientation) {
        if (disabled) return

        pagedMarkerPositions.clear()
        val drawnRects = mutableListOf<RectF>()
        val drawnMarkerIds = HashSet<UUID>()
        var maxPageThisFrame = 0
        var currentPageAfterScreenRotation = Int.MAX_VALUE

        fun drawMarker(marker: ARMarker, lastDrawn: Boolean) {
            val pagedMarker = pagedMarkers[marker.wrapped.id] ?: return

            val pagedPosition =
                pagedPositionOf(
                    pagedMarker = pagedMarker,
                    requireAlreadyCalculated = lastDrawn && !firstFrame
                )
            marker.y = pagedPosition.y
            storeMarkerPosition(pagedMarker)
            pagedMarker.pagedPosition?.page?.let {
                if (it > maxPageThisFrame) maxPageThisFrame = it
            }
            if (firstFrame &&
                    lastDrawnMarkerIds.contains(marker.wrapped.id) &&
                    pagedPosition.page < currentPageAfterScreenRotation
            ) {
                currentPageAfterScreenRotation = pagedPosition.page
            }
            if (pagedMarker.pagedPosition?.page != currentPage) return

            drawnMarkerIds.add(marker.wrapped.id)

            val markerRect = marker.rectF
            val canvasRect = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
            if (!RectF.intersects(canvasRect, markerRect)) return

            canvas.drawTitleText(marker, markerRect)
            canvas.drawDistanceText(marker, markerRect)

            drawnRects.add(markerRect)
        }

        val (lastDrawnMarkers, newlyAppearedMarkers) =
            markers.partition {
                lastDrawnMarkerIds.contains(it.wrapped.id) &&
                    pagedMarkers[it.wrapped.id]?.pagedPosition != null
            }
        lastDrawnMarkers.forEach { drawMarker(it, lastDrawn = true) }
        newlyAppearedMarkers.forEach { drawMarker(it, lastDrawn = false) }

        maxPage = maxPageThisFrame
        if (firstFrame) currentPage = currentPageAfterScreenRotation
        if (currentPage > maxPage) currentPage = maxPage

        lastDrawnMarkerIds = drawnMarkerIds
        markersDrawnStateFlow.value = MarkersDrawn(currentPage, maxPage)
        drawnRectsStateFlow.value = drawnRects
        firstFrame = false
    }

    override fun onSaveInstanceState(): Bundle =
        bundleOf(SavedStateKeys.LAST_DRAWN_MARKER_IDS.name to lastDrawnMarkerIds)

    override fun onRestoreInstanceState(bundle: Bundle?) {
        @Suppress("UNCHECKED_CAST")
        lastDrawnMarkerIds =
            bundle?.getSerializable(SavedStateKeys.LAST_DRAWN_MARKER_IDS.name) as? HashSet<UUID>
                ?: return
    }

    @MainThread
    fun setMarkers(markers: Collection<ARMarker>) {
        if (pagedMarkers.keys.containsAll(markers.map { it.wrapped.id }) &&
                pagedMarkers.size == markers.size
        ) {
            return
        }
        pagedMarkers.clear()
        markers.forEach { marker -> pagedMarkers[marker.wrapped.id] = PagedMarker(marker) }
        currentPage = 0
    }

    internal fun isOnCurrentPage(marker: ARMarker): Boolean =
        pagedMarkers[marker.wrapped.id]?.pagedPosition?.page == currentPage

    private fun pagedPositionOf(
        pagedMarker: PagedMarker,
        requireAlreadyCalculated: Boolean
    ): PagedPosition {
        if (requireAlreadyCalculated) {
            return requireNotNull(pagedMarker.pagedPosition) {
                "Last drawn marker's paged position is null."
            }
        }

        val takenPositions =
            pagedMarkerPositions
                .subMap(
                    pagedMarker.wrapped.x - markerWidthPx * MARKER_WIDTH_TAKEN_X_MULTIPLIER,
                    pagedMarker.wrapped.x + markerWidthPx * MARKER_WIDTH_TAKEN_X_MULTIPLIER
                )
                .values
                .flatten()
                .toSet()
        pagedMarker.pagedPosition?.let { if (!takenPositions.contains(it)) return it }

        val baseY = statusBarHeightPx + actionBarHeightPx
        val position = PagedPosition(baseY, 0)
        var row = 0
        while (takenPositions.contains(position)) {
            position.y += markerHeightPx + MARKER_VERTICAL_SPACING_PX
            ++row
            if (row >= numberOfRows) {
                row = 0
                position.y = baseY
                ++position.page
            }
        }
        pagedMarker.pagedPosition = position
        return position
    }

    private fun storeMarkerPosition(marker: PagedMarker) {
        val pagedPosition =
            marker.pagedPosition
                ?: throw IllegalArgumentException("Marker must have a PagedPosition.")
        val existingMarkerSet = pagedMarkerPositions[marker.wrapped.x]
        existingMarkerSet?.add(pagedPosition)
            ?: run { pagedMarkerPositions[marker.wrapped.x] = mutableSetOf(pagedPosition) }
    }

    private fun Canvas.drawTitleText(marker: ARMarker, rect: RectF) {
        drawMultilineText(
            text = marker.wrapped.name,
            textPaint = titleTextPaint,
            width = (rect.width() - MARKER_PADDING_DP * 2 - ELLIPSIS_WIDTH_PX).toInt(),
            x = marker.x - markerWidthPx / 2 + markerPaddingPx,
            y = marker.y - markerHeightPx / 2 + markerPaddingPx,
            ellipsize = TextUtils.TruncateAt.END,
            maxLines = 2,
        )
    }

    private fun Canvas.drawDistanceText(marker: ARMarker, rect: RectF) {
        val distance =
            TextUtils.ellipsize(
                marker.distance.preciseFormattedDistance,
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

    private class PagedMarker(
        val wrapped: ARMarker,
        var pagedPosition: PagedPosition? = null,
    ) {
        override fun equals(other: Any?): Boolean =
            this === other || (other is PagedMarker && other.wrapped == wrapped)

        override fun hashCode(): Int = Objects.hash(wrapped)
    }

    private data class PagedPosition(var y: Float, var page: Int)

    data class MarkersDrawn(val currentPage: Int, val maxPage: Int)

    private enum class SavedStateKeys {
        LAST_DRAWN_MARKER_IDS,
    }

    companion object {
        private const val MARKER_WIDTH_TAKEN_X_MULTIPLIER = 2.75f
        private const val MARKER_VERTICAL_SPACING_PX = 50f
        private const val NUMBER_OF_ROWS_PORTRAIT = 5
        private const val NUMBER_OF_ROWS_LANDSCAPE = 2
        private const val MARKER_WIDTH_DIVISOR_PORTRAIT = 2
        private const val MARKER_WIDTH_DIVISOR_LANDSCAPE = 4
        private const val MARKER_PADDING_DP = 10f
        private const val ELLIPSIS_WIDTH_PX = 10f
        private const val MARKER_TITLE_TEXT_SIZE_SP = 16f
        private const val MARKER_DISTANCE_TEXT_SIZE_SP = 14f
    }
}
