package com.lookaround.ui.place.map.list

import com.lookaround.core.android.model.Marker

interface PlaceMapListFragmentHost {
    fun onPlaceMapItemClick(marker: Marker)
    fun onShowMapClick()
}
