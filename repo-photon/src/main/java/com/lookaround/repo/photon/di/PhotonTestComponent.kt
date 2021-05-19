package com.lookaround.repo.photon.di

import com.lookaround.core.android.di.NetworkModule
import com.lookaround.repo.photon.PhotonEndpoints
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [PhotonNetworkModule::class, NetworkModule::class])
interface PhotonTestComponent {
    fun photonEndpoints(): PhotonEndpoints
}
