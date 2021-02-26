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
                LocationFactory.create(41.383973, 2.147291, 240.0),
            ))
        markers.add(
            SimpleARMarker(
                "Bfdsafsaf",
                LocationFactory.create(41.383635, 2.147591, 30.0),
            ))
        markers.add(
            SimpleARMarker(
                "Cfdsafsa",
                LocationFactory.create(41.382379, 2.147548, 37.0),
            ))
        markers.add(
            SimpleARMarker(
                "Ddsafsafdaf",
                LocationFactory.create(41.386936, 2.147055, 25.0),
            ))
        markers.add(
            SimpleARMarker(
                "Edsafs",
                LocationFactory.create(41.389077, 2.147248, 34.0),
            ))
        markers.add(
            SimpleARMarker(
                "Ffdsafsafas",
                LocationFactory.create(41.414490, 2.179167, 34.0),
            ))
        markers.add(
            SimpleARMarker(
                "Gdfsafsafas",
                LocationFactory.create(41.412090, 2.188952, 74.0),
            ))
        markers.add(
            SimpleARMarker(
                "Hfdasfsafd",
                LocationFactory.create(41.403011, 2.196333, 25.0),
            ))
        markers.add(
            SimpleARMarker(
                "Iadasfas",
                LocationFactory.create(41.408001, 2.198221, 17.0),
            ))
        markers.add(
            SimpleARMarker(
                "Jadfsfas",
                LocationFactory.create(41.411091, 2.196933, 39.0),
            ))
        markers.add(
            SimpleARMarker(
                "K",
                LocationFactory.create(41.406265, 2.203071, 45.0),
            ))
        markers.add(
            SimpleARMarker(
                "L",
                LocationFactory.create(41.403206, 2.208134, 120.0),
            ))
        markers.add(
            SimpleARMarker(
                "M",
                LocationFactory.create(41.401951, 2.194530, 70.0),
            ))
        markers.add(
            SimpleARMarker(
                "N",
                LocationFactory.create(41.395157, 2.187965, 43.0),
            ))
        markers.add(
            SimpleARMarker(
                "O",
                LocationFactory.create(41.400696, 2.176463, 23.0),
            ))
        markers.add(
            SimpleARMarker(
                "P",
                LocationFactory.create(41.407776, 2.185389, 63.0),
            ))
        markers.add(
            SimpleARMarker(
                "Q   ",
                LocationFactory.create(41.412701, 2.195217, 57.0),
            ))
        markers.add(
            SimpleARMarker(
                "R   ",
                LocationFactory.create(41.409485, 2.198350, 78.0),
            ))
        markers.add(
            SimpleARMarker(
                "S   ",
                LocationFactory.create(41.407066, 2.202642, 33.0),
            ))
        markers.add(
            SimpleARMarker(
                "T   ",
                LocationFactory.create(41.405521, 2.205946, 32.0),
            ))
        markers.add(
            SimpleARMarker(
                "U   ",
                LocationFactory.create(41.402657, 2.199766, 75.0),
            ))
        markers.add(
            SimpleARMarker(
                "V   ",
                LocationFactory.create(41.400276, 2.199165, 256.0),
            ))
        markers.add(
            SimpleARMarker(
                "W   ",
                LocationFactory.create(41.403076, 2.192856, 45.0),
            ))
        markers.add(
            SimpleARMarker(
                "X   ",
                LocationFactory.create(41.399536, 2.190625, 66.0),
            ))
        markers.add(
            SimpleARMarker(
                "Y   ",
                LocationFactory.create(41.402946, 2.189896, 55.0),
            ))
        markers.add(
            SimpleARMarker(
                "Z   ",
                LocationFactory.create(41.401047, 2.184703, 77.0),
            ))
        markers.add(
            SimpleARMarker(
                "0   ",
                LocationFactory.create(41.403336, 2.178180, 33.0),
            ))
        markers.add(
            SimpleARMarker(
                "1   ",
                LocationFactory.create(41.407970, 2.180926, 44.0),
            ))
        markers.add(
            SimpleARMarker(
                "2   ",
                LocationFactory.create(41.412540, 2.179295, 64.0),
            ))
        markers.add(
            SimpleARMarker(
                "3   ",
                LocationFactory.create(41.413280, 2.190282, 32.0),
            ))
        markers.add(
            SimpleARMarker(
                "4   ",
                LocationFactory.create(41.411446, 2.187706, 99.0),
            ))
        return markers
    }
}
