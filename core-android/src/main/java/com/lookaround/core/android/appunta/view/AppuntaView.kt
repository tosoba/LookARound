package com.lookaround.core.android.appunta.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.lookaround.core.android.appunta.marker.CameraMarker
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.renderer.MarkerRenderer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

abstract class AppuntaView : View {
    var location: Location = Location("")
        set(value) {
            field = value
            calculateDistances(cameraMarkers, field)
        }
    var maxDistance: Double = DEFAULT_MAX_DISTANCE
        set(value) {
            field = value
            invalidate()
        }
    var cameraMarkers: List<CameraMarker> = emptyList()
    var onPointPressedListener: OnPointPressedListener? = null
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
        calculateDistances(cameraMarkers, location)
        preRender(canvas)
        for (point in cameraMarkers) {
            calculatePointCoordinates(point)
            if (shouldDraw(point)) {
                point.renderer?.drawPoint(point, canvas, orientation)
                    ?: markerRenderer?.drawPoint(point, canvas, orientation)
            }
        }
        postRender(canvas)
    }

    protected open fun shouldDraw(cameraMarker: CameraMarker): Boolean =
        cameraMarker.distance < maxDistance && cameraMarker.isDrawn

    override fun onTouchEvent(event: MotionEvent): Boolean {
        onPointPressedListener?.let { listener ->
            if (event.action != MotionEvent.ACTION_DOWN) return@let
            findNearestPoint(event.x, event.y)
                ?.takeIf { point -> abs(point.x - event.x) < 50 && abs(point.y - event.y) < 50 }
                ?.let(listener::onPointPressed)
        }
        return super.onTouchEvent(event)
    }

    /**
     * Given a screen coordinate, returns the nearest point to that coordinate
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return The nearest point to coordinate X,Y
     */
    private fun findNearestPoint(x: Float, y: Float): CameraMarker? {
        var nearest: CameraMarker? = null
        var nearestPointDistance = width.coerceAtLeast(height).toDouble()
        for (point in cameraMarkers) {
            val distance =
                sqrt((point.x - x).toDouble().pow(2.0) + (point.y - y).toDouble().pow(2.0))
            if (distance < nearestPointDistance) {
                nearestPointDistance = distance
                nearest = point
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
     * This method will be called for each point on the rendering process, in order to determine
     * where this point should be drawn in the screen
     *
     * @param cameraMarker The point to calculate
     */
    protected abstract fun calculatePointCoordinates(cameraMarker: CameraMarker)

    /**
     * This is the last method called during the painting process. It's used to draw the foreground
     * layer
     *
     * @param canvas The canvas where to draw
     */
    protected abstract fun postRender(canvas: Canvas)

    protected fun getAngle(cameraMarker: CameraMarker): Double =
        atan2(
            cameraMarker.marker.location.latitude - location.latitude,
            cameraMarker.marker.location.longitude - location.longitude)

    /**
     * * Calculate the distance from a given point to all the points stored and sets the distance
     * property for all them
     *
     * @param location Latitude and longitude of the given point
     */
    private fun calculateDistances(cameraMarkers: List<CameraMarker>, location: Location) {
        cameraMarkers.forEach { marker ->
            marker.distance = marker.marker.location.distanceTo(location).toDouble()
        }
    }

    /** This interface represents an object able to be called when a point is pressed */
    interface OnPointPressedListener {
        fun onPointPressed(cameraMarker: CameraMarker)
    }

    companion object {
        /** The default max distance that will be shown if not changed */
        private const val DEFAULT_MAX_DISTANCE = 1000.0
    }
}
