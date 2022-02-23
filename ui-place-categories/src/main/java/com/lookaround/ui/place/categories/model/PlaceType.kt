package com.lookaround.ui.place.categories.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.lookaround.core.model.IPlaceType

@Immutable data class PlaceType(val wrapped: IPlaceType, @DrawableRes val drawableId: Int)
