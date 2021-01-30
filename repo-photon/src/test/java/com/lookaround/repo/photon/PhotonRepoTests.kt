package com.lookaround.repo.photon

import com.lookaround.repo.photon.di.DaggerPhotonComponent
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PhotonRepoTests {
    private val repo: PhotonRepo = DaggerPhotonComponent
        .builder()
        .build()
        .photonRepo()

    @Test
    fun search() {
        runBlocking { println(repo.searchPoints("Berlin").toString()) }
    }
}