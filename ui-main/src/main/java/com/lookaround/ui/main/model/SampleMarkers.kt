package com.lookaround.ui.main.model

import com.lookaround.core.android.model.LocationFactory
import com.lookaround.core.android.model.Marker

object SampleMarkers {
    fun get(): List<Marker> =
        listOf(
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.253004, 20.940759, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.264851, 20.947542, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.253005, 20.940761, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.264803, 20.947562, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.253004, 20.940758, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.264804, 20.947563, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.253006, 20.940756, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.264802, 20.947561, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.253005, 20.940757, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.264801, 20.947560, 30.0),
            ),
        )
}
