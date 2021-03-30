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
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.153005, 21.040761, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.164803, 21.047562, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.153004, 21.040758, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.164804, 21.047563, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.153006, 21.040756, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.164802, 21.047561, 30.0),
            ),
            Marker(
                "KEN CENTER",
                LocationFactory.create(52.153005, 21.040757, 30.0),
            ),
            Marker(
                "SGGW",
                LocationFactory.create(52.164801, 21.047560, 30.0),
            ),
        )
}
