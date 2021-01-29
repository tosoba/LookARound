package com.lookaround.core.di

import com.lookaround.core.di.annotation.TestHttpClient
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import sun.net.www.http.HttpClient
import javax.inject.Singleton

@Module
object HttpModule {
    @Provides
    @Singleton
    fun httpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor()
        .apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    @TestHttpClient
    fun testHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(loggingInterceptor)
        .build()
}