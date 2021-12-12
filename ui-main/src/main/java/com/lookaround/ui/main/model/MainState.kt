package com.lookaround.ui.main.model

import android.location.Location
import android.os.Parcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.ui.main.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(
    val markers: Loadable<ParcelableSortedSet<Marker>> = Empty,
    val locationState: Loadable<Location> = Empty,
    val lastLiveBottomSheetState: Int = BottomSheetBehavior.STATE_HIDDEN,
    val selectedBottomNavigationViewItemId: Int = R.id.action_unchecked,
    val recentSearchesCount: Int = 0,
    val searchMode: MainSearchMode = MainSearchMode.AUTOCOMPLETE,
    val autocompleteSearchQuery: String = "",
    val placeTypesSearchQuery: String = "",
    val placeListSearchQuery: String = "",
    val recentSearchQuery: String = "",
    val searchFocused: Boolean = false,
) : Parcelable {
    internal fun copyWithLocationException(throwable: Throwable): MainState =
        copy(locationState = locationState.copyWithError(throwable))
}
