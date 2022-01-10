package com.lookaround.core.android.di

import com.lookaround.core.android.repo.AppRepo
import com.lookaround.core.repo.IAppRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.beryukhov.reactivenetwork.ReactiveNetwork

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds abstract fun appRepo(repo: AppRepo): IAppRepo

    companion object {
        @Provides @Singleton fun reactiveNetwork(): ReactiveNetwork = ReactiveNetwork()
    }
}
