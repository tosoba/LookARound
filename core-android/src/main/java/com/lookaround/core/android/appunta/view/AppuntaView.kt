package com.lookaround.core.android.appunta.view

import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.PointRenderer
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/***
 *
 * This is the base class in order to create Views using the Appunta system.
 * The class has all needed calculations and values to retrieve info from points.
 *
 * It's important to understand how this will work. All the stuff happens in the onDraw Method.
 *
 * The [.onDraw] method has three phases: **preRender**, **pointRendering** & **postRender**.
 *
 *  * The **preRender** phase triggers the method [.preRender], used to draw all needed elements used in
 * the background.
 *
 *  * In the **pointRendering** phase, the method calculatePointCoordinates(SimplePoint) is invoked per each on of the points,
 * in order to calculate the screen coordinates for each one of them. Then, they are painted by calling
 * their PaintRenderer.
 *
 *  * In the **Post render** phase, the [.postRender] method is invoked in order to paint
 * the foreground layer.
 *
 */
abstract class AppuntaView : View {
    var location: Location = Location("")
        set(value) {
            field = value
            calculateDistances(points, field)
        }
    var maxDistance = DEFAULT_MAX_DISTANCE
        set(value) {
            field = value
            invalidate()
        }
    var points: List<Point> = emptyList()
    var onPointPressedListener: OnPointPressedListener? = null
    var pointRenderer: PointRenderer? = null
    var orientation: Orientation = Orientation()
        set(value) {
            field = value
            invalidate()
        }
    var phoneRotation = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calculateDistances(points, location)
        preRender(canvas)
        for (point in points) {
            calculatePointCoordinates(point)
            if (shouldDraw(point)) {
                point.renderer?.drawPoint(point, canvas, orientation)
                    ?: pointRenderer?.drawPoint(point, canvas, orientation)
            }
        }
        postRender(canvas)
    }

    protected open fun shouldDraw(point: Point): Boolean =
        point.distance < maxDistance && point.isDrawn

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
    private fun findNearestPoint(x: Float, y: Float): Point? {
        var nearest: Point? = null
        var minorDistance = width.coerceAtLeast(height).toDouble()
        for (point in points) {
            val distance =
                sqrt((point.x - x).toDouble().pow(2.0) + (point.y - y).toDouble().pow(2.0))
            if (distance < minorDistance) {
                minorDistance = distance
                nearest = point
            }
        }
        return nearest
    }

    /**
     * This is the first method called during the painting process. It's used to draw the background layer
     *
     * @param canvas The canvas where to draw
     */
    protected abstract fun preRender(canvas: Canvas)

    /**
     * This method will be called for each point on the rendering process, in order to determine where this point
     * should be drawn in the screen
     *
     * @param point The point to calculate
     */
    protected abstract fun calculatePointCoordinates(point: Point)

    /**
     * This is the last method called during the painting process. It's used to draw the foreground layer
     *
     * @param canvas The canvas where to draw
     */
    protected abstract fun postRender(canvas: Canvas)

    protected fun getAngle(point: Point): Double = atan2(
        point.location.latitude - location.latitude,
        point.location.longitude - location.longitude
    )

    /***
     * Calculate the distance from a given point to all the points stored and
     * sets the distance property for all them
     *
     * @param location
     * Latitude and longitude of the given point
     */
    private fun calculateDistances(points: List<Point>, location: Location) {
        points.forEach { poi -> poi.distance = poi.location.distanceTo(location).toDouble() }
    }

    /**
     * This interface represents an object able to be called when a point is pressed
     */
    interface OnPointPressedListener {
        fun onPointPressed(point: Point)
    }

    companion object {
        /**
         * The default max distance that will be shown if not changed
         */
        private const val DEFAULT_MAX_DISTANCE = 1000.0
    }
}