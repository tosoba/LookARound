package com.lookaround.repo.overpass.di

import android.content.Context
import com.dropbox.android.external.store4.Store
import com.lookaround.core.android.ext.buildRoom
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.IPlacesRepo
import com.lookaround.repo.overpass.OverpassDatabase
import com.lookaround.repo.overpass.OverpassEndpoints
import com.lookaround.repo.overpass.OverpassRepo
import com.lookaround.repo.overpass.OverpassSearchAroundStore
import com.lookaround.repo.overpass.dao.SearchAroundDao
import com.lookaround.repo.overpass.entity.SearchAroundInput
import com.lookaround.repo.overpass.mapper.NodeEntityMapper
import com.lookaround.repo.overpass.mapper.NodeMapper
import dagger.Binds
import dagger.Module
import dagger.Provides
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
abstract class OverpassRepoModule {
    @Binds abstract fun overpassRepo(overpassRepo: OverpassRepo): IPlacesRepo

    companion object {
        @Provides
        @Singleton
        fun overpassDatabase(@ApplicationContext context: Context): OverpassDatabase =
            context.buildRoom()

        @Provides
        @Singleton
        fun overpassSearchAroundStore(
            searchAroundDao: SearchAroundDao,
            endpoints: OverpassEndpoints,
            nodeMapper: NodeMapper,
            nodeEntityMapper: NodeEntityMapper
        ): Store<SearchAroundInput, List<NodeDTO>> =
            OverpassSearchAroundStore.build(
                searchAroundDao,
                endpoints,
                nodeMapper,
                nodeEntityMapper
            )

        @Provides
        @Singleton
        fun searchAroundDao(db: OverpassDatabase): SearchAroundDao = db.searchAroundDao()
    }
}
