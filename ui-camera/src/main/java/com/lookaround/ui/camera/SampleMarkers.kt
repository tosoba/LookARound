package com.lookaround.ui.camera

import com.lookaround.core.android.appunta.location.LocationFactory
import com.lookaround.core.android.appunta.marker.CameraMarker
import com.lookaround.core.android.appunta.marker.SimpleCameraMarker
import java.util.*

object SampleMarkers {
    fun get(): List<CameraMarker> {
        val cameraMarkers: MutableList<CameraMarker> = ArrayList()
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.383973, 2.147291, 240.0),
                "Adfadfsaf",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.383635, 2.147591, 30.0),
                "Bfdsafsaf",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.382379, 2.147548, 37.0),
                "Cfdsafsa",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.386936, 2.147055, 25.0),
                "Ddsafsafdaf",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.389077, 2.147248, 34.0),
                "Edsafs",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.414490, 2.179167, 34.0),
                "Ffdsafsafas",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.412090, 2.188952, 74.0),
                "Gdfsafsafas",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.403011, 2.196333, 25.0),
                "Hfdasfsafd",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.408001, 2.198221, 17.0),
                "Iadasfas",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.411091, 2.196933, 39.0),
                "Jadfsfas",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.406265, 2.203071, 45.0),
                "K",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.403206, 2.208134, 120.0),
                "L",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.401951, 2.194530, 70.0),
                "M",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.395157, 2.187965, 43.0),
                "N",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.400696, 2.176463, 23.0),
                "O",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.407776, 2.185389, 63.0),
                "P",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.412701, 2.195217, 57.0),
                "Q   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.409485, 2.198350, 78.0),
                "R   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.407066, 2.202642, 33.0),
                "S   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.405521, 2.205946, 32.0),
                "T   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.402657, 2.199766, 75.0),
                "U   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.400276, 2.199165, 256.0),
                "V   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.403076, 2.192856, 45.0),
                "W   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.399536, 2.190625, 66.0),
                "X   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.402946, 2.189896, 55.0),
                "Y   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.401047, 2.184703, 77.0),
                "Z   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.403336, 2.178180, 33.0),
                "0   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.407970, 2.180926, 44.0),
                "1   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.412540, 2.179295, 64.0),
                "2   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.413280, 2.190282, 32.0),
                "3   ",
            ))
        cameraMarkers.add(
            SimpleCameraMarker(
                LocationFactory.create(41.411446, 2.187706, 99.0),
                "4   ",
            ))
        return cameraMarkers
    }
}
