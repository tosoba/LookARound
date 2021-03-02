package com.lookaround.repo.photon

import com.github.filosganga.geogson.model.Point
import com.google.gson.JsonPrimitive
import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.PlacesAutocompleteRepo
import javax.inject.Inject

class PhotonRepo @Inject constructor(private val photonEndpoints: PhotonEndpoints) :
    PlacesAutocompleteRepo {
    override suspend fun searchPoints(query: String): List<PointDTO> =
        photonEndpoints
            .search(query)
            .features()
            ?.filter {
                val properties = it.properties()
                if (properties?.containsKey("name") != true) return@filter false
                val name = properties.getValue("name")
                name is JsonPrimitive && name.isString && it.geometry() is Point
            }
            ?.map {
                val point = it.geometry() as Point
                PointDTO(
                    name = it.properties().getValue("name").asJsonPrimitive.asString,
                    lat = point.lat(),
                    lng = point.lon()
                )
            }
            ?: emptyList()
}
