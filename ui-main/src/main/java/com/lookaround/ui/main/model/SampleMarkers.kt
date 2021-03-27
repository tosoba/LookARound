package com.lookaround.ui.main.model

import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.Marker

object SampleMarkers {
    fun get(): List<Marker> =
        listOf(
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.153004, 21.040759, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.164851, 21.047542, 30.0),
            )
        )
}
