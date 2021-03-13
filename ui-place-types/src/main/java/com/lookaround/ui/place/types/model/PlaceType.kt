package com.lookaround.ui.place.types.model

import androidx.compose.runtime.Immutable
import com.lookaround.core.model.IPlaceType

@Immutable data class PlaceType(val wrapped: IPlaceType, val imageUrl: String)
