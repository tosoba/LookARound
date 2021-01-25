package com.lookaround.repo.nominatim.mapper

import org.mapstruct.Qualifier

@Qualifier
@Target(AnnotationTarget.FUNCTION)
annotation class ElementsArrayToMap
