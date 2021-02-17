package com.lookaround.core.android.appunta.renderer.impl

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import com.lookaround.core.android.appunta.location.LocationFactory
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.point.ARObject
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.point.PointsUtil
import com.lookaround.core.android.appunta.renderer.PointRenderer
import java.util.*
import kotlin.math.abs

class RectViewRenderer : PointRenderer {
    override fun drawPoint(point: Point, canvas: Canvas, orientation: Orientation) {
        var y = baseY
        val objectToDraw: ARObject = ARObject.findByPoint(point) ?: return
        objectToDraw.screenY?.let { screenY ->
            y = screenY
        } ?: run {
            y = findBestY(getTakenYsByBearing(objectToDraw))
            objectToDraw.screenY = y
        }
        point.y = y

        val rect = RectF(
            point.x - dialogWidth / 2,
            point.y - dialogHeight / 2,
            point.x + dialogWidth / 2,
            point.y + dialogHeight / 2
        )

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            color = Color.parseColor("#70ffffff")
        }
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
            textSize = 40f
            textAlign = Paint.Align.LEFT
            isLinearText = true
        }
        val width = (rect.width() - 10).toInt() // 10 to keep some space on the right for the "..."
        val txt = TextUtils.ellipsize("The loooooong text", textPaint, width.toFloat(), TextUtils.TruncateAt.END)
        canvas.drawText(txt, 0, txt.length, point.x - dialogWidth / 2 + textYOffset, point.y, textPaint)
        canvas.drawRoundRect(rect, 10f, 10f, backgroundPaint)
    }

    private fun getTakenYsByBearing(thisPoint: ARObject): List<Float> {
        val takenYs = ArrayList<Float>()
        val userLocation = LocationFactory.create(41.383873, 2.156574, 12.0)
        val bearingThis = PointsUtil.calculateBearing(userLocation, thisPoint.location)
        for (arObject in ARObject.getObjects()) {
            if (arObject == thisPoint) continue
            arObject.screenY?.let { screenY ->
                val bearingCurrent = PointsUtil.calculateBearing(userLocation, arObject.location)
                if (abs(bearingCurrent - bearingThis) < 30.0 && !takenYs.contains(screenY)) {
                    takenYs.add(screenY)
                }
            } ?: continue
        }
        return takenYs
    }

    private fun findBestY(takenYs: List<Float>): Float {
        var bestY = baseY
        while (takenYs.contains(bestY)) {
            bestY += dialogHeight
        }
        return bestY
    }

    companion object {
        private const val baseY = 100f
        private const val dialogHeight = 100f
        private const val dialogWidth = 400f
        private const val textYOffset = 20f
    }
}