package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.renderer.MarkerRenderer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

abstract class ARView : View {
    var location: Location = Location("")
        set(value) {
            field = value
            calculateDistances(markers, field)
        }
    var maxDistance: Double = DEFAULT_MAX_DISTANCE
        set(value) {
            field = value
            invalidate()
        }
    var markers: List<ARMarker> = emptyList()
    var onMarkerPressedListener: OnMarkerPressedListener? = null
    var markerRenderer: MarkerRenderer? = null
    var orientation: Orientation = Orientation()
        set(value) {
            field = value
            invalidate()
        }
    var phoneRotation: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calculateDistances(markers, location)
        preRender(canvas)
        for (marker in markers) {
            calculateMarkerCoordinates(marker)
            if (shouldDraw(marker)) {
                marker.renderer?.draw(marker, canvas, orientation)
                    ?: markerRenderer?.draw(marker, canvas, orientation)
            }
        }
        postRender(canvas)
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

    /**
     * Given a screen coordinate, returns the nearest marker to that coordinate
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return The nearest marker to coordinate X,Y
     */
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

    /**
     * This is the first method called during the painting process. It's used to draw the background
     * layer
     *
     * @param canvas The canvas where to draw
     */
    protected abstract fun preRender(canvas: Canvas)

    /**
     * This method will be called for each marker on the rendering process, in order to determine
     * where this marker should be drawn in the screen
     *
     * @param marker The marker to calculate
     */
    protected abstract fun calculateMarkerCoordinates(marker: ARMarker)

    /**
     * This is the last method called during the painting process. It's used to draw the foreground
     * layer
     *
     * @param canvas The canvas where to draw
     */
    protected abstract fun postRender(canvas: Canvas)

    protected fun getAngle(marker: ARMarker): Double =
        atan2(
            marker.wrapped.location.latitude - location.latitude,
            marker.wrapped.location.longitude - location.longitude)

    /**
     * * Calculate the distance from a given marker to all the markers stored and sets the distance
     * property for all them
     *
     * @param location Latitude and longitude of the given marker
     */
    private fun calculateDistances(markers: List<ARMarker>, location: Location) {
        markers.forEach { marker ->
            marker.distance = marker.wrapped.location.distanceTo(location).toDouble()
        }
    }

    /** This interface represents an object able to be called when a marker is pressed */
    interface OnMarkerPressedListener {
        fun onMarkerPressed(marker: ARMarker)
    }

    companion object {
        /** The default max distance that will be shown if not changed */
        private const val DEFAULT_MAX_DISTANCE = 1000.0
    }
}
