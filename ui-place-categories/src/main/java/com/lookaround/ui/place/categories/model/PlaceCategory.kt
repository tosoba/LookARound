package com.lookaround.ui.place.categories.model

import androidx.compose.runtime.Immutable

@Immutable data class PlaceCategory(val name: String, val placeTypes: List<PlaceType>)
