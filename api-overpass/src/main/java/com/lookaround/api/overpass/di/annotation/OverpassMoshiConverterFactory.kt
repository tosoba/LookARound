package com.lookaround.api.overpass.di.annotation

import javax.inject.Qualifier

@Qualifier
@MustBeDocumented
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class OverpassMoshiConverterFactory(val value: String = "OverpassMoshi")