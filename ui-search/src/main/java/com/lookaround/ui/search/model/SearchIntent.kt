package com.lookaround.ui.search.model

import android.location.Location

sealed class SearchIntent {
    data class SearchPlaces(val query: String, val priorityLocation: Location?) : SearchIntent()
}
