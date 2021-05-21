package com.lookaround.repo.overpass.di

import com.lookaround.core.android.di.NetworkModule
import com.lookaround.repo.overpass.OverpassEndpoints
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [OverpassNetworkModule::class, NetworkModule::class])
interface OverpassTestComponent {
    fun overpassEndpoints(): OverpassEndpoints
}
