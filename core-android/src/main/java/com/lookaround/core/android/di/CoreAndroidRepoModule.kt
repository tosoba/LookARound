package com.lookaround.core.android.di

import com.lookaround.core.android.repo.AppRepo
import com.lookaround.core.repo.IAppRepo
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreAndroidRepoModule {
    @Binds abstract fun appRepo(appRepo: AppRepo): IAppRepo
}
