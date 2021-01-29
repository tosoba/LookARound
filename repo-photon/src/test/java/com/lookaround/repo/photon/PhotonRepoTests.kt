package com.lookaround.repo.photon

import com.lookaround.repo.photon.di.DaggerPhotonComponent
import com.lookaround.repo.photon.PhotonEndpoints
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PhotonRepoTests {
    private val endpoints: PhotonEndpoints = DaggerPhotonComponent
        .builder()
        .build()
        .photonEndpoints()

    @Test
    fun search() {
        runBlocking { println(endpoints.search("Berlin").toString()) }
    }
}