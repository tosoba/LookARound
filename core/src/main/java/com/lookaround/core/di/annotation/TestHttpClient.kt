package com.lookaround.core.di.annotation

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class TestHttpClient(val value: String = "TestHttpClient")