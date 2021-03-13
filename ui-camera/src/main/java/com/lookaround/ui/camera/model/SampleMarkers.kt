package com.lookaround.ui.camera.model

import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker

object SampleMarkers {
    fun get(): List<ARMarker> =
        listOf(
            SimpleARMarker(
                "KEN CENTER",
                LocationFactory.create(52.153004, 21.040759, 240.0),
            ),
            SimpleARMarker(
                "SGGW",
                LocationFactory.create(52.164851, 21.047542, 30.0),
            )
        )
}