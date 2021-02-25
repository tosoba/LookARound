package com.lookaround.core.android.appunta.math3d

import android.location.Location
import android.view.Surface
import com.lookaround.core.android.appunta.orientation.Orientation
import kotlin.math.cos

object Math3dUtil {
    // Check
    // http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
    private const val METERS_IN_A_DEGREE = 111111.0
    private const val QUADRANT = Math.PI / 2
    private val viewPortPos = Vector2()

    /**
     * This method transforms the camera orientation angles into 3D space angles. It also calculates
     * angle Z separately, as it is a local angle, not an universal one (We want to move our head,
     * not the whole universe)
     *
     * @param inOrientation Input parameter. The orientation of the camera
     * @param inPhoneRotation Input parameter. The current camera rotation. This is a int constant
     * from Surface class with four possible values
     * @param outCamRot Output parameter. The real camera rotation in our 3D Space
     * @param outCamTrig Output parameter. The trigonometric calculations of all three angles
     * @param outScreenRot Output parameter. The rotation angle of the screen (local Z angle)
     * @param outScreenRotTrig Output parameter. The trigonometric calculations of the angle.
     */
    fun getCamRotation(
        inOrientation: Orientation,
        inPhoneRotation: Int,
        outCamRot: Vector3,
        outCamTrig: Trig3,
        outScreenRot: Vector1,
        outScreenRotTrig: Trig1
    ) {
        // X goes the other way
        outCamRot.x = -inOrientation.x.toDouble()
        // 0 value is south, so turn around
        outCamRot.y = inOrientation.y + Math.PI
        // The universal rotation angle for Z is always 0
        outCamRot.z = 0.0

        // The value of the z angle (screen rotation)
        if (inPhoneRotation == Surface.ROTATION_0) {
            outScreenRot.v = -inOrientation.z.toDouble()
        }
        if (inPhoneRotation == Surface.ROTATION_180) {
            outScreenRot.v = -inOrientation.z + Math.PI
        }
        if (inPhoneRotation == Surface.ROTATION_90) {
            outScreenRot.v = -inOrientation.z - QUADRANT
        }
        if (inPhoneRotation == Surface.ROTATION_270) {
            outScreenRot.v = -inOrientation.z + QUADRANT
        }
        outCamTrig.setVector3(outCamRot)
        outScreenRotTrig.setVector1(outScreenRot)
    }

    /**
     * Transforms a Location type into a position in space (currently is direct attribution) just to
     * move from lat/lon/alt to z,x,y
     *
     * @param inLocation Input parameter. A location where the point is currently located.
     * @param outPos A position object with same values.
     */
    fun convertLocationToPosition(inLocation: Location, outPos: Vector3) {
        outPos.z = inLocation.latitude
        outPos.x = inLocation.longitude
        outPos.y = inLocation.altitude
    }

    /**
     * * Calculates de relative position of a point getting the camera position as 0,0,0. Returns
     * the result in meters Check this article:
     * http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
     * @param inPointPos Input parameter. Current position of the point, z is latitude in degrees, y
     * is altitude in meters, x is longitude in degrees
     * @param inCamPos Input parameter. Current position of the camera, z is latitude in degrees, y
     * is in meters, x is longitude in degrees
     * @param outRelativePosInMeters Output parameter. Relative distance of the point to the camera.
     * X and Z are converted to meters
     */
    fun getRelativeTranslationInMeters(
        inPointPos: Vector3,
        inCamPos: Vector3,
        outRelativePosInMeters: Vector3
    ) {
        outRelativePosInMeters.z = (inCamPos.z - inPointPos.z) * METERS_IN_A_DEGREE
        outRelativePosInMeters.y = inCamPos.y - inPointPos.y
        outRelativePosInMeters.x =
            (inCamPos.x - inPointPos.x) * cos(inCamPos.z - inPointPos.z) * METERS_IN_A_DEGREE
    }

    /**
     * * Check this article before trying to only understand a simple comma
     * http://en.wikipedia.org/wiki/3D_projection#Perspective_projection In fact, I don't care too
     * much about the formula. Just C&P it.
     * @param inRelativePos Input parameter. The coordinates of a point relative to camera position
     * in meters
     * @param inCamTrig Input parameter. The set of trigonometric calculations of the camera angles
     * @param outRelativeRotPos Output parameter. The final coordinates of the point after rotating
     * the space in order to set the camera to angles 0,0,0
     */
    fun getRelativeRotation(inRelativePos: Vector3, inCamTrig: Trig3, outRelativeRotPos: Vector3) {
        // Check this article before trying to only understand a simple comma
        // http://en.wikipedia.org/wiki/3D_projection#Perspective_projection
        // In fact, I don't care too much about the formula. Just C&P it.
        outRelativeRotPos.x =
            (inCamTrig.yCos *
                (inCamTrig.zSin * inRelativePos.y + inCamTrig.zCos * inRelativePos.x) -
                inCamTrig.ySin * inRelativePos.z)
        outRelativeRotPos.y =
            (inCamTrig.xSin *
                (inCamTrig.yCos * inRelativePos.z +
                    inCamTrig.ySin *
                        (inCamTrig.zSin * inRelativePos.y + inCamTrig.zCos * inRelativePos.x)) +
                inCamTrig.xCos *
                    (inCamTrig.zCos * inRelativePos.y - inCamTrig.zSin * inRelativePos.x))
        outRelativeRotPos.z =
            (inCamTrig.xCos *
                (inCamTrig.yCos * inRelativePos.z +
                    inCamTrig.ySin *
                        (inCamTrig.zSin * inRelativePos.y + inCamTrig.zCos * inRelativePos.x)) -
                inCamTrig.xSin *
                    (inCamTrig.zCos * inRelativePos.y - inCamTrig.zSin * inRelativePos.x))
    }

    /**
     * @param inRelativePos Input parameter.
     * @param inScreenSize Input parameter.
     * @param inScreenRatio Input parameter.
     * @param inScreenRotTrig Input parameter.
     * @param outScreenPos Output parameter.
     * @return drawn
     */
    fun convert3dTo2d(
        inRelativePos: Vector3,
        inScreenSize: Vector2,
        inScreenRatio: Vector3,
        inScreenRotTrig: Trig1,
        outScreenPos: Vector2
    ): Boolean =
        if (inRelativePos.z > 0) {
            viewPortPos.x = inRelativePos.x * inScreenRatio.x / (inScreenRatio.z * inRelativePos.z)
            viewPortPos.y = inRelativePos.y * inScreenRatio.y / (inScreenRatio.z * inRelativePos.z)
            outScreenPos.x =
                inScreenSize.x / 2 + viewPortPos.x * inScreenRotTrig.cos -
                    viewPortPos.y * inScreenRotTrig.sin
            outScreenPos.y =
                inScreenSize.y / 2 +
                    viewPortPos.y * inScreenRotTrig.cos +
                    viewPortPos.x * inScreenRotTrig.sin
            true
        } else {
            false
        }
}
