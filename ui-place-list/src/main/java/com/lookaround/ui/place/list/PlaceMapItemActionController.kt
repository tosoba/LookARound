package com.lookaround.ui.place.list

import com.lookaround.core.android.model.Marker

interface PlaceMapItemActionController {
    fun onPlaceMapItemClick(marker: Marker)
}