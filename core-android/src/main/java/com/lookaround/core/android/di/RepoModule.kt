package com.lookaround.core.android.di

import com.lookaround.core.android.repo.AppRepo
import com.lookaround.core.repo.IAppRepo
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.repo.nominatim.di.NominatimModule
import com.lookaround.repo.overpass.di.OverpassModule
import com.lookaround.repo.photon.PhotonRepo
import com.lookaround.repo.photon.di.PhotonModule
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Module(includes = [OverpassModule::class, NominatimModule::class, PhotonModule::class])
@InstallIn(SingletonComponent::class)
abstract class RepoModule {
    @Binds abstract fun appRepo(repo: AppRepo): IAppRepo
    @Binds abstract fun placesAutoCompleteRepo(repo: PhotonRepo): IPlacesAutocompleteRepo
}
