package com.lookaround.repo.overpass.di

import com.lookaround.core.di.CoreNetworkModule
import com.lookaround.repo.overpass.OverpassRepo
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [OverpassModule::class, CoreNetworkModule::class])
interface OverpassComponent {
    fun overpassService(): OverpassRepo
}
