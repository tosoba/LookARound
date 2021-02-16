package com.lookaround.core.android.appunta.point

import android.location.Location
import com.lookaround.core.android.appunta.renderer.PointRenderer
import java.util.*

interface Point {
    /***
     * Distance to a point
     * @return the distance in Km if previously set
     */
    var distance: Double

    /***
     * Name of the point. Created in order to make your life easier
     * @return the name of the point
     */
    var name: String

    /***
     * Gets the name of the renderer to use to draw this point
     * @return The renderer
     */
    var renderer: PointRenderer?

    /***
     * A unique id
     * @return an id
     */
    var id: UUID

    /***
     * Last X coordinate where the point should be drawn
     * @return X coordinate of the canvas
     */
    var x: Float

    /***
     * Last Y coordinate where the point should be drawn
     * @return Y coordinate of the canvas
     */
    var y: Float
    var location: Location
    var isSelected: Boolean
    var isDrawn: Boolean
}