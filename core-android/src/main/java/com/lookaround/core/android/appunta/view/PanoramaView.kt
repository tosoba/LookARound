package com.lookaround.core.android.appunta.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.lookaround.core.android.appunta.point.Point

class PanoramaView : AppuntaView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun preRender(canvas: Canvas) = Unit

    override fun calculatePointCoordinates(point: Point) {
        val angularDistance = angleDifference(
            Math.toRadians(orientation.x.toDouble()),
            MAX_DEGREES / 4 - getAngle(point)
        )
        point.x = ((angularDistance + VISIBLE_DEGREES / 2) * width / VISIBLE_DEGREES).toFloat()
        point.y = (height - height * point.distance / maxDistance).toFloat()
    }

    override fun postRender(canvas: Canvas) = Unit

    private fun angleDifference(centered: Double, moved: Double): Double {
        val cwDiff = cwDifference(centered, moved)
        val ccwDiff = ccwDiference(centered, moved)
        return if (cwDiff < ccwDiff) cwDiff else -ccwDiff
    }

    private fun cwDifference(centered: Double, moved: Double): Double {
        var cw = moved - centered
        if (cw < 0) {
            cw += MAX_DEGREES
        }
        return cw
    }

    private fun ccwDiference(centered: Double, moved: Double): Double {
        var ccw = centered - moved
        if (ccw < 0) {
            ccw += MAX_DEGREES
        }
        return ccw
    }

    companion object {
        private const val VISIBLE_DEGREES = Math.PI / 3
        private const val MAX_DEGREES = Math.PI * 2
    }
}