package com.lookaround.repo.photon

import com.github.filosganga.geogson.model.Feature
import com.lookaround.core.model.PointDTO
import com.lookaround.repo.photon.di.DaggerPhotonTestComponent
import com.lookaround.repo.photon.ext.namedPointsOnly
import com.lookaround.repo.photon.ext.pointDTO
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PhotonEndpointsTests {
    private val endpoints: PhotonEndpoints =
        DaggerPhotonTestComponent.builder().build().photonEndpoints()

    @Test
    fun search() {
        runBlocking {
            println(
                endpoints
                    .search("Berlin", priorityLat = null, priorityLon = null)
                    .features()
                    ?.namedPointsOnly
                    ?.map(Feature::pointDTO)
                    ?: emptyList<PointDTO>()
            )
        }
    }
}
