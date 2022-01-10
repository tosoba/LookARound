package com.lookaround.repo.photon.di

import android.content.Context
import com.dropbox.android.external.store4.Store
import com.lookaround.core.android.ext.buildRoom
import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IAutocompleteSearchRepo
import com.lookaround.repo.photon.PhotonAutocompleteSearchStore
import com.lookaround.repo.photon.PhotonDatabase
import com.lookaround.repo.photon.PhotonEndpoints
import com.lookaround.repo.photon.PhotonRepo
import com.lookaround.repo.photon.dao.AutocompleteSearchDao
import com.lookaround.repo.photon.entity.AutocompleteSearchInput
import com.lookaround.repo.photon.mapper.PointEntityMapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotonRepoModule {
    @Binds abstract fun placesAutoCompleteRepo(repo: PhotonRepo): IAutocompleteSearchRepo

    companion object {
        @Provides
        @Singleton
        fun overpassDatabase(@ApplicationContext context: Context): PhotonDatabase =
            context.buildRoom()

        @Provides
        @Reusable
        fun photonAutocompleteSearchStore(
            autocompleteSearchDao: AutocompleteSearchDao,
            endpoints: PhotonEndpoints,
            pointEntityMapper: PointEntityMapper
        ): Store<AutocompleteSearchInput, List<PointDTO>> =
            PhotonAutocompleteSearchStore.build(autocompleteSearchDao, endpoints, pointEntityMapper)

        @Provides
        fun autocompleteSearchDao(db: PhotonDatabase): AutocompleteSearchDao =
            db.autocompleteSearchDao()
    }
}
