package com.lookaround.core.android.di

import com.lookaround.core.android.repo.AppRepo
import com.lookaround.core.repo.IAppRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.beryukhov.reactivenetwork.ReactiveNetwork
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreAndroidModule {
    @Binds
    abstract fun appRepo(appRepo: AppRepo): IAppRepo

    companion object {
        @Provides
        @Singleton
        fun reactiveNetwork(): ReactiveNetwork = ReactiveNetwork()
    }
}
