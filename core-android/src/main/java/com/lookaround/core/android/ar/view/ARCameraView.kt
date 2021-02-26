package com.lookaround.core.android.ar.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.math3d.*

class ARCameraView : ARView {
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

    override fun preRender(canvas: Canvas) {
        // For the moment we set a square as ratio. Size is arithmetic mean of width and height
        screenRatio.y = ((width + height).toFloat() / 2).toDouble()
        screenRatio.x = ((width + height).toFloat() / 2).toDouble()
        // Get the current size of the window
        screenSize.y = height.toDouble()
        screenSize.x = width.toDouble()
        // Obtain the current camera rotation and related calculations based on phone orientation
        // and rotation
        Math3D.getCamRotation(
            orientation, phoneRotation, camRot, camTrig, screenRot, screenRotTrig)
        // Transform current camera location into a position object;
        Math3D.convertLocationToPosition(location, camPos)
    }

    override fun calculateMarkerCoordinates(marker: ARMarker) {
        // Transform marker Location into a Position object
        Math3D.convertLocationToPosition(marker.wrapped.location, markerPos)
        // Calculate relative position to the camera. Transforms angles of latitude and longitude
        // into meters of distance.
        Math3D.getRelativeTranslationInMeters(markerPos, camPos, relativePos)
        // Rotates the marker around the camera in order to set the camera rotation to <0,0,0>
        Math3D.getRelativeRotation(relativePos, camTrig, relativeRotPos)
        // Converts a 3d position into a 2d position on screen
        val drawn =
            Math3D.convert3dTo2d(
                relativeRotPos, screenSize, screenRatio, screenRotTrig, screenPos)
        // If drawn is false, the marker is behind us, so no need to paint
        if (drawn) {
            marker.x = screenPos.x.toFloat()
            marker.y = screenPos.y.toFloat()
        }
        marker.isDrawn = drawn
    }

    override fun postRender(canvas: Canvas) = Unit

    companion object {
        private const val SCREEN_DEPTH = 1
    }
}
