package com.lookaround.repo.photon.di

import com.lookaround.core.di.HttpModule
import com.lookaround.repo.photon.PhotonEndpoints
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [PhotonModule::class, HttpModule::class])
interface PhotonComponent {
    fun photonEndpoints(): PhotonEndpoints
}
