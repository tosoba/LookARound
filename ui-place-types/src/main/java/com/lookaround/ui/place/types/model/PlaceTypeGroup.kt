package com.lookaround.ui.place.types.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlaceTypeGroup(
    val name: String,
    val placeTypes: List<PlaceType>
)