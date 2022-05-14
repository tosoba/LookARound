package com.lookaround.core.android.map

import android.content.Context
import android.graphics.PointF
import android.location.Location
import androidx.core.content.ContextCompat
import com.lookaround.core.android.R
import com.lookaround.core.android.ext.asBitmapDrawable
import com.lookaround.core.android.ext.lngLat
import com.lookaround.core.android.ext.pxToDp
import com.mapzen.tangram.MapController
import com.mapzen.tangram.Marker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/** Takes care of showing the location + direction + accuracy marker on the map */
class UserLocationMapComponent(context: Context, private val controller: MapController) {
    // markers showing the user's location, direction and accuracy of location
    private val locationMarker: Marker
    private val accuracyMarker: Marker
    private val directionMarker: Marker

    private val directionMarkerSize: PointF

    /**
     * Whether the whole thing is visible. True by default. It is only visible if both this flag is
     * true and location is not null.
     */
    var isVisible: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (!value) hide() else show()
        }

    /** The location of the GPS location dot on the map. Null if none (yet) */
    var location: Location? = null
        set(value) {
            if (field == value) return
            field = value
            updateLocation()
        }

    /** The view rotation angle in degrees. Null if not set (yet) */
    var rotation: Double? = null
        set(value) {
            if (field == value) return
            field = value
            updateDirection()
        }

    /**
     * Tell this component the current map zoom. Why does it need to know this at all? It doesn't,
     * but it needs to know when it changed. There is no specific event for that. Whenever the zoom
     * changed, the marker showing the accuracy must be updated because the accuracy's marker size
     * is calculated programmatically using the current zoom.
     */
    var currentMapZoom: Float? = null
        set(value) {
            if (field == value) return
            field = value
            updateAccuracy()
        }

    init {
        val dotImg =
            ContextCompat.getDrawable(context, R.drawable.location_dot)!!.asBitmapDrawable(
                context.resources
            )
        val dotSize =
            PointF(
                context.pxToDp(dotImg.intrinsicWidth.toFloat()),
                context.pxToDp(dotImg.intrinsicHeight.toFloat())
            )

        val directionImg =
            ContextCompat.getDrawable(context, R.drawable.location_direction)!!.asBitmapDrawable(
                context.resources
            )
        directionMarkerSize =
            PointF(
                context.pxToDp(directionImg.intrinsicWidth.toFloat()),
                context.pxToDp(directionImg.intrinsicHeight.toFloat())
            )

        val accuracyImg =
            ContextCompat.getDrawable(context, R.drawable.accuracy_circle)!!.asBitmapDrawable(
                context.resources
            )

        locationMarker =
            controller.addMarker().apply {
                setStylingFromString(
                    """{
                        style: 'points',
                        color: 'white',
                        size: [${dotSize.x}px, ${dotSize.y}px],
                        order: 2000,
                        flat: true,
                        collide: false,
                        interactive: true
                    }""".trimIndent()
                )
                setDrawable(dotImg)
                setDrawOrder(10)
                isVisible = true
            }

        directionMarker =
            controller.addMarker().apply {
                setDrawable(directionImg)
                setDrawOrder(9)
                isVisible = true
            }

        accuracyMarker =
            controller.addMarker().apply {
                setDrawable(accuracyImg)
                setDrawOrder(8)
                isVisible = true
            }
    }

    private fun hide() {
        locationMarker.isVisible = false
        accuracyMarker.isVisible = false
        directionMarker.isVisible = false
    }

    private fun show() {
        updateLocation()
        updateDirection()
    }

    /** Update the GPS position shown on the map */
    private fun updateLocation() {
        if (!isVisible) return
        val pos = location?.lngLat ?: return

        accuracyMarker.isVisible = true
        accuracyMarker.setPointEased(pos, 600, MapController.EaseType.CUBIC)
        locationMarker.isVisible = true
        locationMarker.setPointEased(pos, 600, MapController.EaseType.CUBIC)
        directionMarker.isVisible = rotation != null
        directionMarker.setPointEased(pos, 600, MapController.EaseType.CUBIC)

        updateAccuracy()
    }

    /** Update the circle that shows the GPS accuracy on the map */
    private fun updateAccuracy() {
        if (!isVisible) return
        val location = location ?: return

        val size =
            location.accuracy * pixelsPerMeter(location.latitude, controller.cameraPosition.zoom)
        accuracyMarker.setStylingFromString(
            """{
            style: 'points',
            color: 'white',
            size: ${size}px,
            order: 2000,
            flat: true,
            collide: false
        }""".trimIndent()
        )
    }

    /** Update the marker that shows the direction in which the smartphone is held */
    private fun updateDirection() {
        if (!isVisible) return
        // no sense to display direction if there is no location yet
        if (rotation == null || location == null) return

        directionMarker.isVisible = true
        directionMarker.setStylingFromString(
            """{
            style: 'points',
            color: '#cc536dfe',
            size: [${directionMarkerSize.x}px, ${directionMarkerSize.y}px],
            order: 2000,
            collide: false,
            flat: true,
            angle: $rotation
        }""".trimIndent()
        )
    }

    private fun pixelsPerMeter(latitude: Double, zoom: Float): Double {
        val numberOfTiles = (2.0).pow(zoom.toDouble())
        val metersPerTile = cos(latitude * PI / 180.0) * EARTH_CIRCUMFERENCE / numberOfTiles
        return 256 / metersPerTile
    }

    companion object {
        const val EARTH_CIRCUMFERENCE = 40000000.0
    }
}
