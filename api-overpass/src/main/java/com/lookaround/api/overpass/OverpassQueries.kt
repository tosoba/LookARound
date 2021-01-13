package com.lookaround.api.overpass

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import hu.supercluster.overpasser.library.output.OutputFormat
import hu.supercluster.overpasser.library.output.OutputModificator
import hu.supercluster.overpasser.library.output.OutputOrder
import hu.supercluster.overpasser.library.output.OutputVerbosity
import hu.supercluster.overpasser.library.query.OverpassFilterQuery
import hu.supercluster.overpasser.library.query.OverpassQuery
import kotlin.math.sqrt

object OverpassQueries {
    fun findAttractions(
        lat: Double, lng: Double, radiusInMeters: Double
    ): String = filterNodeQuery()
        .tag("tourism", "attraction")
        .boundingBox(lat, lng, radiusInMeters)
        .buildWithoutQuotes()

    fun findPlacesOfType(
        amenity: String, lat: Double, lng: Double, radiusInMeters: Double
    ): String = filterNodeQuery()
        .amenity(amenity)
        .boundingBox(lat, lng, radiusInMeters)
        .buildWithoutQuotes()

    fun findImages(
        lat: Double, lng: Double, radiusInMeters: Double
    ): String = filterNodeQuery()
        .tagRegex("image", "http")
        .boundingBox(lat, lng, radiusInMeters)
        .buildWithoutQuotes()

    private fun filterNodeQuery(
        format: OutputFormat = OutputFormat.JSON, timeoutInSeconds: Int = 10
    ): OverpassFilterQuery = OverpassQuery()
        .format(format)
        .timeout(timeoutInSeconds)
        .filterQuery()
        .node()

    private fun OverpassFilterQuery.buildWithoutQuotes(): String = end()
        .output(OutputVerbosity.BODY, OutputModificator.CENTER, OutputOrder.QT, 100)
        .build()
        .replace("\"", "")

    private fun OverpassFilterQuery.boundingBox(
        lat: Double, lng: Double, radiusInMeters: Double
    ): OverpassFilterQuery {
        val latLng = LatLng(lat, lng)
        val distanceFromCenterToCorner = radiusInMeters * sqrt(2.0)
        val southWest = SphericalUtil.computeOffset(latLng, distanceFromCenterToCorner, 225.0)
        val northEast = SphericalUtil.computeOffset(latLng, distanceFromCenterToCorner, 45.0)
        return boundingBox(
            southWest.latitude,
            southWest.longitude,
            northEast.latitude,
            northEast.longitude
        )
    }
}
