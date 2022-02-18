package com.lookaround.ui.place.map.list

import com.lookaround.core.android.model.Marker

interface PlaceMapListHost {
    val initialItemBackground: ItemBackground
    fun onPlaceMapItemClick(marker: Marker)
    fun onShowMapClick()

    enum class ItemBackground {
        OPAQUE,
        TRANSPARENT
    }
}
