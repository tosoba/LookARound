package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.MainThread
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import kotlinx.parcelize.Parcelize

abstract class ARView<R : MarkerRenderer> : View {
    open var povLocation: Location? = null
        @MainThread
        set(value) {
            field = value
            value?.let { calculateDistancesBetween(it, markers) }
            maxRange =
                (markers.lastOrNull()?.distance?.toDouble()
                    ?: DEFAULT_MAX_RANGE_METERS) * RANGE_MARGIN_MULTIPLIER
        }
    protected var maxRange: Double = DEFAULT_MAX_RANGE_METERS
        @MainThread
        set(value) {
            field = value
            invalidate()
        }
    var markers: List<ARMarker> = emptyList()
        @MainThread
        set(value) {
            field = value
            povLocation?.let { calculateDistancesBetween(it, value) }
            maxRange =
                (value.lastOrNull()?.distance?.toDouble()
                    ?: DEFAULT_MAX_RANGE_METERS) * RANGE_MARGIN_MULTIPLIER
        }
    var markerRenderer: R? = null
        @MainThread set
    var orientation: Orientation = Orientation()
        @MainThread
        set(value) {
            field = value
            invalidate()
        }
    var phoneRotation: Int = 0
        @MainThread set

    protected val markerWidth: Float
        get() = markerRenderer?.markerWidthPx ?: MarkerRenderer.DEFAULT_MARKER_DIMENSION_PX
    protected val markerHeight: Float
        get() = markerRenderer?.markerHeightPx ?: MarkerRenderer.DEFAULT_MARKER_DIMENSION_PX

    protected abstract val ARMarker.shouldBeDrawn: Boolean

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val povLocation = this.povLocation ?: return
        preDraw(canvas, povLocation)
        val markerRenderer = this.markerRenderer ?: return
        markers.forEach { marker -> calculateMarkerScreenPosition(marker, povLocation) }
        markerRenderer.draw(markers.filter { it.shouldBeDrawn }, canvas, orientation)
        postDraw(canvas, povLocation)
    }

    protected abstract fun preDraw(canvas: Canvas, location: Location)
    protected abstract fun calculateMarkerScreenPosition(marker: ARMarker, location: Location)
    protected abstract fun postDraw(canvas: Canvas, location: Location)

    private fun calculateDistancesBetween(location: Location, markers: List<ARMarker>) {
        markers.forEach { marker -> marker.distance = marker.wrapped.location.distanceTo(location) }
    }

    override fun onSaveInstanceState(): Parcelable? =
        SavedState(super.onSaveInstanceState(), markerRenderer?.onSaveInstanceState())

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as? SavedState
        super.onRestoreInstanceState(savedState?.superSavedState ?: state)
        markerRenderer?.onRestoreInstanceState(savedState?.rendererBundle)
    }

    @Parcelize
    internal class SavedState(
        val superSavedState: Parcelable?,
        val rendererBundle: Bundle?,
    ) : BaseSavedState(superSavedState), Parcelable

    companion object {
        const val DEFAULT_MAX_RANGE_METERS = 1_000.0
        const val RANGE_MARGIN_MULTIPLIER = 1.1
    }
}
