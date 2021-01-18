package com.lookaround.api.overpass.di

import com.lookaround.api.overpass.OverpassService
import com.lookaround.core.di.HttpModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [OverpassModule::class, HttpModule::class])
interface OverpassComponent {
    fun overpassService(): OverpassService
}