package com.lookaround.api.overpass

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import hu.supercluster.overpasser.library.output.OutputFormat
import hu.supercluster.overpasser.library.output.OutputModificator
import hu.supercluster.overpasser.library.output.OutputOrder
import hu.supercluster.overpasser.library.output.OutputVerbosity
import hu.supercluster.overpasser.library.query.OverpassQuery

object OverpassQueries {
    fun findNearbyAttractions(lat: Double, lng: Double, radiusInMeters: Long): String {
        val southWest = SphericalUtil
            .computeOffset(LatLng(lat, lng), radiusInMeters.toDouble(), 225.0)
        val northEast = SphericalUtil
            .computeOffset(LatLng(lat, lng), radiusInMeters.toDouble(), 45.0)
        return OverpassQuery()
            .format(OutputFormat.JSON)
            .timeout(10)
            .filterQuery()
            .node()
            .tag("tourism", "attraction")
            .boundingBox(
                southWest.latitude,
                southWest.longitude,
                northEast.latitude,
                northEast.longitude
            )
            .end()
            .output(OutputVerbosity.BODY, OutputModificator.CENTER, OutputOrder.QT, 100)
            .build()
    }
}
