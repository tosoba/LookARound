package com.lookaround.ui.place.categories.model

import androidx.annotation.DrawableRes
import com.lookaround.core.model.IPlaceType

data class PlaceType(val wrapped: IPlaceType, @DrawableRes val drawableId: Int)
