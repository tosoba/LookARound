package com.lookaround.ui.place.map.list

import com.lookaround.core.android.model.Marker

interface PlaceMapItemActionController {
    fun onPlaceMapItemClick(marker: Marker)
}
