package com.lookaround.api.overpass

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import hu.supercluster.overpasser.adapter.OverpassQueryResult

fun OverpassQueryResult.elementsWithinRadius(
    lat: Double, lng: Double, radiusInMeters: Double
): List<OverpassQueryResult.Element> {
    val ll = LatLng(lat, lng)
    return elements?.filter {
        SphericalUtil.computeDistanceBetween(ll, LatLng(it.lat, it.lon)) <= radiusInMeters
    } ?: emptyList()
}
