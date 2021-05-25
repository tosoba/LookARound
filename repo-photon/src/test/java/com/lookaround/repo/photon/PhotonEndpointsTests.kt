package com.lookaround.repo.photon

import com.github.filosganga.geogson.model.Feature
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lookaround.repo.photon.di.DaggerPhotonTestComponent
import com.lookaround.repo.photon.ext.namedPointsOnly
import com.lookaround.repo.photon.ext.pointDTO
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PhotonEndpointsTests {
    private val endpoints: PhotonEndpoints =
        DaggerPhotonTestComponent.builder().build().photonEndpoints()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun search() {
        runBlocking {
            val features =
                endpoints
                    .search("Aleje", priorityLat = null, priorityLon = null, limit = 500)
                    .features()
                    ?.namedPointsOnly
                    ?.distinctBy { it.properties().getValue("name").asString }
                    ?.take(50)
            println(gson.toJson(features))

            val points = features?.map(Feature::pointDTO) ?: emptyList()
            println(gson.toJson(points))
        }
    }
}
