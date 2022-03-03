package com.lookaround.ui.place.categories.model

import androidx.annotation.DrawableRes
import com.lookaround.core.model.IPlaceType

sealed interface PlaceTypeListItem {
    data class PlaceCategory(val name: String) : PlaceTypeListItem

    data class PlaceType(
        val wrapped: IPlaceType,
        @DrawableRes val drawableId: Int,
    ) : PlaceTypeListItem
}
