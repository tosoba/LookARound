package com.lookaround.repo.photon.di

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class PhotonGsonConverterFactory(val value: String = "PhotonGsonConverterFactory")
