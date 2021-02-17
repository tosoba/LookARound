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
                LocationFactory.create(41.383973, 2.147291, 240.0),
                "Adfadfsaf",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.383635, 2.147591, 30.0),
                "Bfdsafsaf",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.382379, 2.147548, 37.0),
                "Cfdsafsa",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.386936, 2.147055, 25.0),
                "Ddsafsafdaf",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.389077, 2.147248, 34.0),
                "Edsafs",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.414490, 2.179167, 34.0),
                "Ffdsafsafas",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.412090, 2.188952, 74.0),
                "Gdfsafsafas",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.403011, 2.196333, 25.0),
                "Hfdasfsafd",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.408001, 2.198221, 17.0),
                "Iadasfas",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.411091, 2.196933, 39.0),
                "Jadfsfas",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.406265, 2.203071, 45.0),
                "K",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.403206, 2.208134, 120.0),
                "L",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.401951, 2.194530, 70.0),
                "M",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.395157, 2.187965, 43.0),
                "N",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.400696, 2.176463, 23.0),
                "O",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.407776, 2.185389, 63.0),
                "P",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.412701, 2.195217, 57.0),
                "Q   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.409485, 2.198350, 78.0),
                "R   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.407066, 2.202642, 33.0),
                "S   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.405521, 2.205946, 32.0),
                "T   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.402657, 2.199766, 75.0),
                "U   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.400276, 2.199165, 256.0),
                "V   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.403076, 2.192856, 45.0),
                "W   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.399536, 2.190625, 66.0),
                "X   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.402946, 2.189896, 55.0),
                "Y   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.401047, 2.184703, 77.0),
                "Z   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.403336, 2.178180, 33.0),
                "0   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.407970, 2.180926, 44.0),
                "1   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.412540, 2.179295, 64.0),
                "2   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.413280, 2.190282, 32.0),
                "3   ",
                renderer
            )
        )
        points.add(
            SimplePoint(
                UUID.randomUUID(),
                LocationFactory.create(41.411446, 2.187706, 99.0),
                "4   ",
                renderer
            )
        )
        return points
    }
}