package com.lookaround.ui.place.map.list

import android.view.View
import com.lookaround.core.android.model.Marker

interface PlaceMapItemActionController {
    fun onPlaceMapItemClick(marker: Marker, view: View)
}
