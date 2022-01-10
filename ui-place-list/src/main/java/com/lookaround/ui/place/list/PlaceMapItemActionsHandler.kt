package com.lookaround.ui.place.list

import com.lookaround.core.android.model.Marker

interface PlaceMapItemActionsHandler {
    fun onPlaceMapItemClick(marker: Marker)
}
