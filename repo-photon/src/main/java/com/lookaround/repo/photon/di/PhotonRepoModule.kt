package com.lookaround.repo.photon.di

import android.content.Context
import com.dropbox.android.external.store4.Store
import com.lookaround.core.android.ext.buildRoom
import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.repo.photon.PhotonAutocompleteSearchStore
import com.lookaround.repo.photon.PhotonDatabase
import com.lookaround.repo.photon.PhotonEndpoints
import com.lookaround.repo.photon.PhotonRepo
import com.lookaround.repo.photon.entity.AutocompleteSearchInput
import com.lookaround.repo.photon.mapper.PointEntityMapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@FlowPreview
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotonRepoModule {
    @Binds abstract fun placesAutoCompleteRepo(repo: PhotonRepo): IPlacesAutocompleteRepo

    companion object {
        @Provides
        @Singleton
        fun overpassDatabase(@ApplicationContext context: Context): PhotonDatabase =
            context.buildRoom()

        @Provides
        @Singleton
        fun photonAutocompleteSearchStore(
            db: PhotonDatabase,
            endpoints: PhotonEndpoints,
            pointEntityMapper: PointEntityMapper
        ): Store<AutocompleteSearchInput, List<PointDTO>> =
            PhotonAutocompleteSearchStore.build(
                db.autocompleteSearchDao(),
                endpoints,
                pointEntityMapper,
            )
    }
}
