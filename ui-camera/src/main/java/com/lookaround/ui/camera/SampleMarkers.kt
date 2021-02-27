package com.lookaround.ui.camera

import com.lookaround.core.android.ar.location.LocationFactory
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker
import java.util.*

object SampleMarkers {
    fun get(): List<ARMarker> {
        val markers: MutableList<ARMarker> = ArrayList()
        markers.add(
            SimpleARMarker(
                "Adfadfsaf",
                LocationFactory.create(52.383973, 21.047291, 240.0),
            ))
        markers.add(
            SimpleARMarker(
                "Bfdsafsaf",
                LocationFactory.create(52.383635, 21.047591, 30.0),
            ))
        markers.add(
            SimpleARMarker(
                "Cfdsafsa",
                LocationFactory.create(52.382379, 21.047548, 37.0),
            ))
        markers.add(
            SimpleARMarker(
                "Ddsafsafdaf",
                LocationFactory.create(52.386936, 21.047055, 25.0),
            ))
        markers.add(
            SimpleARMarker(
                "Edsafs",
                LocationFactory.create(52.389077, 21.047248, 34.0),
            ))
        markers.add(
            SimpleARMarker(
                "Ffdsafsafas",
                LocationFactory.create(52.414490, 21.079167, 34.0),
            ))
        markers.add(
            SimpleARMarker(
                "Gdfsafsafas",
                LocationFactory.create(52.412090, 21.088952, 74.0),
            ))
        markers.add(
            SimpleARMarker(
                "Hfdasfsafd",
                LocationFactory.create(52.403011, 21.096333, 25.0),
            ))
        markers.add(
            SimpleARMarker(
                "Iadasfas",
                LocationFactory.create(52.408001, 21.098221, 17.0),
            ))
        markers.add(
            SimpleARMarker(
                "Jadfsfas",
                LocationFactory.create(52.411091, 21.096933, 39.0),
            ))
        markers.add(
            SimpleARMarker(
                "K",
                LocationFactory.create(52.406265, 21.003071, 45.0),
            ))
        markers.add(
            SimpleARMarker(
                "L",
                LocationFactory.create(52.403206, 21.008134, 120.0),
            ))
        markers.add(
            SimpleARMarker(
                "M",
                LocationFactory.create(52.401951, 21.094530, 70.0),
            ))
        markers.add(
            SimpleARMarker(
                "N",
                LocationFactory.create(52.395157, 21.087965, 43.0),
            ))
        markers.add(
            SimpleARMarker(
                "O",
                LocationFactory.create(52.400696, 21.076463, 23.0),
            ))
        markers.add(
            SimpleARMarker(
                "P",
                LocationFactory.create(52.407776, 21.085389, 63.0),
            ))
        markers.add(
            SimpleARMarker(
                "Q   ",
                LocationFactory.create(52.412701, 21.095217, 57.0),
            ))
        markers.add(
            SimpleARMarker(
                "R   ",
                LocationFactory.create(52.409485, 21.098350, 78.0),
            ))
        markers.add(
            SimpleARMarker(
                "S   ",
                LocationFactory.create(52.407066, 21.002642, 33.0),
            ))
        markers.add(
            SimpleARMarker(
                "T   ",
                LocationFactory.create(52.405521, 21.005946, 32.0),
            ))
        markers.add(
            SimpleARMarker(
                "U   ",
                LocationFactory.create(52.402657, 21.099766, 75.0),
            ))
        markers.add(
            SimpleARMarker(
                "V   ",
                LocationFactory.create(52.400276, 21.099165, 256.0),
            ))
        markers.add(
            SimpleARMarker(
                "W   ",
                LocationFactory.create(52.403076, 21.092856, 45.0),
            ))
        markers.add(
            SimpleARMarker(
                "X   ",
                LocationFactory.create(52.399536, 21.090625, 66.0),
            ))
        markers.add(
            SimpleARMarker(
                "Y   ",
                LocationFactory.create(52.402946, 21.089896, 55.0),
            ))
        markers.add(
            SimpleARMarker(
                "Z   ",
                LocationFactory.create(52.401047, 21.084703, 77.0),
            ))
        markers.add(
            SimpleARMarker(
                "0   ",
                LocationFactory.create(52.403336, 21.078180, 33.0),
            ))
        markers.add(
            SimpleARMarker(
                "1   ",
                LocationFactory.create(52.407970, 21.080926, 44.0),
            ))
        markers.add(
            SimpleARMarker(
                "2   ",
                LocationFactory.create(52.412540, 21.079295, 64.0),
            ))
        markers.add(
            SimpleARMarker(
                "3   ",
                LocationFactory.create(52.413280, 21.090282, 32.0),
            ))
        markers.add(
            SimpleARMarker(
                "4   ",
                LocationFactory.create(52.411446, 21.087706, 99.0),
            ))
        return markers
    }
}
