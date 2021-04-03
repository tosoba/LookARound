package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.MainThread
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

abstract class ARView<R : MarkerRenderer> : View {
    open var povLocation: Location? = null
        @MainThread
        set(value) {
            field = value
            calculateDistancesTo(requireNotNull(value), markers)
        }
    var maxDistance: Double = DEFAULT_MAX_DISTANCE_METERS
        @MainThread
        set(value) {
            field = value
            invalidate()
        }
    var markers: List<ARMarker> = emptyList()
        @MainThread
        set(value) {
            field = value
            povLocation?.let { calculateDistancesTo(it, value) }
        }
    var onMarkerPressedListener: OnMarkerPressedListener? = null
        @MainThread set
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
        preRender(canvas, povLocation)
        for (marker in markers) {
            calculateMarkerScreenPosition(marker, povLocation)
            if (shouldDraw(marker)) {
                marker.renderer?.draw(marker, canvas, orientation)
                    ?: markerRenderer?.draw(marker, canvas, orientation)
            }
        }
        markerRenderer?.postDrawAll()
        postRender(canvas, povLocation)
    }

    protected open fun shouldDraw(marker: ARMarker): Boolean =
        marker.distance < maxDistance && marker.isDrawn

    override fun onTouchEvent(event: MotionEvent): Boolean {
        onMarkerPressedListener?.let { listener ->
            if (event.action != MotionEvent.ACTION_DOWN) return@let
            findNearestMarker(event.x, event.y)
                ?.takeIf { marker -> abs(marker.x - event.x) < 50 && abs(marker.y - event.y) < 50 }
                ?.let(listener::onMarkerPressed)
        }
        return super.onTouchEvent(event)
    }

    private fun findNearestMarker(x: Float, y: Float): ARMarker? {
        var nearest: ARMarker? = null
        var nearestMarkerDistance = width.coerceAtLeast(height).toDouble()
        for (marker in markers) {
            val distance =
                sqrt((marker.x - x).toDouble().pow(2.0) + (marker.y - y).toDouble().pow(2.0))
            if (distance < nearestMarkerDistance) {
                nearestMarkerDistance = distance
                nearest = marker
            }
        }
        return nearest
    }

    protected abstract fun preRender(canvas: Canvas, location: Location)
    protected abstract fun calculateMarkerScreenPosition(marker: ARMarker, location: Location)
    protected abstract fun postRender(canvas: Canvas, location: Location)

    protected fun getAngleBetween(marker: ARMarker, location: Location): Double =
        atan2(
            marker.wrapped.location.latitude - location.latitude,
            marker.wrapped.location.longitude - location.longitude
        )

    private fun calculateDistancesTo(location: Location, markers: List<ARMarker>) {
        markers.forEach { marker ->
            marker.distance = marker.wrapped.location.distanceTo(location).toDouble()
        }
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

    interface OnMarkerPressedListener {
        fun onMarkerPressed(marker: ARMarker)
    }

    companion object {
        private const val DEFAULT_MAX_DISTANCE_METERS = 1000.0
    }
}
