package com.lookaround.core.android.ar.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.location.Location
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.MainThread
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.math3d.*
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ARCameraView : ARView<CameraMarkerRenderer> {
    private val camRot = Vector3()
    private val camTrig = Trig3()
    private val camPos = Vector3()
    private val markerPos = Vector3()
    private val relativePos = Vector3()
    private val relativeRotPos = Vector3()
    private val screenRatio = Vector3()
    private val screenPos = Vector2()
    private val screenSize = Vector2()
    private val screenRot = Vector1()
    private val screenRotTrig = Trig1()

    override var povLocation: Location?
        get() = super.povLocation
        @MainThread
        set(value) {
            super.povLocation = value
            markerRenderer?.povLocation = value
        }

    var onMarkerPressed: ((ARMarker) -> Unit)? = null
        @MainThread set
    var onTouch: (() -> Unit)? = null
        @MainThread set

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    init {
        screenRatio.z = SCREEN_DEPTH.toDouble()
    }

    override fun preRender(canvas: Canvas, location: Location) {
        // For the moment we set a square as ratio. Size is arithmetic mean of width and height
        screenRatio.y = ((width + height).toFloat() / 2).toDouble()
        screenRatio.x = ((width + height).toFloat() / 2).toDouble()
        // Get the current size of the window
        screenSize.y = height.toDouble()
        screenSize.x = width.toDouble()
        // Obtain the current camera rotation and related calculations based on phone orientation
        // and rotation
        Math3D.getCamRotation(orientation, phoneRotation, camRot, camTrig, screenRot, screenRotTrig)
        // Transform current camera location into a position object;
        Math3D.convertLocationToPosition(location, camPos)
    }

    override fun calculateMarkerScreenPosition(marker: ARMarker, location: Location) {
        // Transform marker Location into a Position object
        Math3D.convertLocationToPosition(marker.wrapped.location, markerPos)
        // Calculate relative position to the camera. Transforms angles of latitude and longitude
        // into meters of distance.
        Math3D.getRelativeTranslationInMeters(markerPos, camPos, relativePos)
        // Rotates the marker around the camera in order to set the camera rotation to <0,0,0>
        Math3D.getRelativeRotation(relativePos, camTrig, relativeRotPos)
        // Converts a 3d position into a 2d position on screen
        val drawn =
            Math3D.convert3dTo2d(relativeRotPos, screenSize, screenRatio, screenRotTrig, screenPos)
        // If drawn is false, the marker is behind us, so no need to paint
        if (drawn) {
            marker.x = screenPos.x.toFloat()
            marker.y = screenPos.y.toFloat()
        }
        marker.isDrawn = drawn
    }

    override fun postRender(canvas: Canvas, location: Location) = Unit

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val markerPressed =
            onMarkerPressed?.let { listener ->
                if (event.action != MotionEvent.ACTION_DOWN) return@let false
                val pressedMarker =
                    findNearestMarker(event.x, event.y)?.takeIf { marker ->
                        abs(marker.x - event.x) < markerWidth / 2 &&
                            abs(marker.y - event.y) < markerHeight / 2
                    }
                if (pressedMarker != null) {
                    listener(pressedMarker)
                    true
                } else {
                    false
                }
            }
                ?: false
        if (!markerPressed) onTouch?.invoke()
        return super.onTouchEvent(event)
    }

    private fun findNearestMarker(x: Float, y: Float): ARMarker? =
        markers
            .filter { marker -> marker.isDrawn && markerRenderer?.isOnCurrentPage(marker) ?: true }
            .minByOrNull { marker ->
                sqrt((marker.x - x).toDouble().pow(2.0) + (marker.y - y).toDouble().pow(2.0))
            }

    companion object {
        private const val SCREEN_DEPTH = 1
    }
}
