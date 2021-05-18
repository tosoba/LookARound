package com.lookaround.repo.overpass.di

import com.lookaround.core.android.di.NetworkModule
import com.lookaround.repo.overpass.OverpassRepo
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [OverpassModule::class, NetworkModule::class])
interface OverpassComponent {
    fun overpassRepo(): OverpassRepo
}
