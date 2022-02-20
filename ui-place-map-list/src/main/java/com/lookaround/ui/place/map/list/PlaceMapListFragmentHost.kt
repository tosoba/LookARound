package com.lookaround.ui.place.map.list

import com.lookaround.core.android.architecture.ListFragmentHost
import com.lookaround.core.android.model.Marker

interface PlaceMapListFragmentHost : ListFragmentHost {
    fun onPlaceMapItemClick(marker: Marker)
    fun onShowMapClick()
}
