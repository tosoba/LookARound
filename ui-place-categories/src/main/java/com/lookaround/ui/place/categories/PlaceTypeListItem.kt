package com.lookaround.ui.place.categories

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.lookaround.core.model.IPlaceType
import kotlinx.parcelize.Parcelize

sealed interface PlaceTypeListItem : Parcelable {
    @Parcelize data class Spacer(val heightPx: Int) : PlaceTypeListItem

    @Parcelize data class PlaceCategory(val name: String) : PlaceTypeListItem

    @Parcelize
    data class PlaceType<PT>(
        val wrapped: PT,
        @DrawableRes val drawableId: Int,
    ) : PlaceTypeListItem where PT : IPlaceType, PT : Parcelable
}
