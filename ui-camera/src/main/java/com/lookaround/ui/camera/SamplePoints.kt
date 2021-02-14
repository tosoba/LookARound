package com.lookaround.ui.camera

import com.lookaround.core.android.appunta.location.LocationFactory
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.point.SimplePoint
import com.lookaround.core.android.appunta.renderer.PointRenderer
import java.util.*

object SamplePoints {
    fun get(renderer: PointRenderer? = null): List<Point> {
        val points: MutableList<Point> = ArrayList()
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.383973, 2.147291, 240.0),
                renderer,
                "Adfadfsaf"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.383635, 2.147591, 30.0),
                renderer,
                "Bfdsafsaf"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.382379, 2.147548, 37.0),
                renderer,
                "Cfdsafsa"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.386936, 2.147055, 25.0),
                renderer,
                "Ddsafsafdaf"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.389077, 2.147248, 34.0),
                renderer,
                "Edsafs"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.414490, 2.179167, 34.0),
                renderer,
                "Ffdsafsafas"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.412090, 2.188952, 74.0),
                renderer,
                "Gdfsafsafas"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.403011, 2.196333, 25.0),
                renderer,
                "Hfdasfsafd"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.408001, 2.198221, 17.0),
                renderer,
                "Iadasfas"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.411091, 2.196933, 39.0),
                renderer,
                "Jadfsfas"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.406265, 2.203071, 45.0),
                renderer,
                "K"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.403206, 2.208134, 120.0),
                renderer,
                "L"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.401951, 2.194530, 70.0),
                renderer,
                "M"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.395157, 2.187965, 43.0),
                renderer,
                "N"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.400696, 2.176463, 23.0),
                renderer,
                "O"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.407776, 2.185389, 63.0),
                renderer,
                "P"
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.412701, 2.195217, 57.0),
                renderer,
                "Q   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.409485, 2.198350, 78.0),
                renderer,
                "R   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.407066, 2.202642, 33.0),
                renderer,
                "S   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.405521, 2.205946, 32.0),
                renderer,
                "T   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.402657, 2.199766, 75.0),
                renderer,
                "U   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.400276, 2.199165, 256.0),
                renderer,
                "V   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.403076, 2.192856, 45.0),
                renderer,
                "W   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.399536, 2.190625, 66.0),
                renderer,
                "X   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.402946, 2.189896, 55.0),
                renderer,
                "Y   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.401047, 2.184703, 77.0),
                renderer,
                "Z   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.403336, 2.178180, 33.0),
                renderer,
                "0   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.407970, 2.180926, 44.0),
                renderer,
                "1   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.412540, 2.179295, 64.0),
                renderer,
                "2   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.413280, 2.190282, 32.0),
                renderer,
                "3   "
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.createLocation(41.411446, 2.187706, 99.0),
                renderer,
                "4   "
            )
        )
        return points
    }
}