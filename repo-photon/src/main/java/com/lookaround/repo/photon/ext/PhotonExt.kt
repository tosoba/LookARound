package com.lookaround.repo.photon.ext

import com.github.filosganga.geogson.model.Feature
import com.github.filosganga.geogson.model.Point
import com.google.gson.JsonPrimitive
import com.lookaround.core.model.PointDTO

internal val List<Feature>.namedPointsOnly: List<Feature>
    get() = filter {
        val properties = it.properties()
        if (properties?.containsKey("name") != true) return@filter false
        val name = properties.getValue("name")
        name is JsonPrimitive && name.isString && it.geometry() is Point
    }

internal val Feature.pointDTO: PointDTO
    get() {
        val point = geometry() as Point
        return PointDTO(
            name = properties().getValue("name").asJsonPrimitive.asString,
            lat = point.lat(),
            lng = point.lon()
        )
    }
