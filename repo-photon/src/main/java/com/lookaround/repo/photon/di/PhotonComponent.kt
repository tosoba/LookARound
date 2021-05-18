package com.lookaround.repo.photon.di

import com.lookaround.core.android.di.NetworkModule
import com.lookaround.repo.photon.PhotonRepo
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [PhotonModule::class, NetworkModule::class])
interface PhotonComponent {
    fun photonRepo(): PhotonRepo
}
