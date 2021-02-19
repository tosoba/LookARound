package com.lookaround.core.android.appunta.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.lookaround.core.android.appunta.math3d.*
import com.lookaround.core.android.appunta.point.Point

class EyeView : AppuntaView {
    private val camRot = Vector3()
    private val camTrig = Trig3()
    private val camPos = Vector3()
    private val pointPos = Vector3()
    private val relativePos = Vector3()
    private val relativeRotPos = Vector3()
    private val screenRatio = Vector3()
    private val screenPos = Vector2()
    private val screenSize = Vector2()
    private val screenRot = Vector1()
    private val screenRotTrig = Trig1()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int)
            : super(context, attrs, defStyle)

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
        //Obtain the current camera rotation and related calculations based on phone orientation and rotation
        Math3dUtil.getCamRotation(
            orientation,
            phoneRotation,
            camRot,
            camTrig,
            screenRot,
            screenRotTrig
        )
        //Transform current camera location into a position object;
        Math3dUtil.convertLocationToPosition(location, camPos)
    }

    override fun calculatePointCoordinates(point: Point) {
        //Transform point Location into a Position object
        Math3dUtil.convertLocationToPosition(point.location, pointPos)
        //Calculate relative position to the camera. Transforms angles of latitude and longitude into meters of distance.
        Math3dUtil.getRelativeTranslationInMeters(pointPos, camPos, relativePos)
        //Rotates the point around the camera in order to set the camera rotation to <0,0,0>
        Math3dUtil.getRelativeRotation(relativePos, camTrig, relativeRotPos)
        //Converts a 3d position into a 2d position on screen
        val drawn = Math3dUtil.convert3dTo2d(
            relativeRotPos,
            screenSize,
            screenRatio,
            screenRotTrig,
            screenPos
        )
        //If drawn is false, the point is behind us, so no need to paint
        if (drawn) {
            point.x = screenPos.x.toFloat()
            point.y = screenPos.y.toFloat()
        }
        point.isDrawn = drawn
    }

    override fun postRender(canvas: Canvas) = Unit

    companion object {
        private const val SCREEN_DEPTH = 1
    }
}