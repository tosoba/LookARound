package com.lookaround.repo.photon.di

import com.lookaround.core.di.CoreNetworkModule
import com.lookaround.repo.photon.PhotonRepo
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [PhotonModule::class, CoreNetworkModule::class])
interface PhotonComponent {
    fun photonRepo(): PhotonRepo
}
