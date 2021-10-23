package com.lookaround.ui.main.model

import android.location.Location
import android.os.Parcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableList
import com.lookaround.ui.main.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(
    val markers: Loadable<ParcelableList<Marker>> = Empty,
    val locationState: Loadable<Location> = Empty,
    val lastLiveBottomSheetState: Int = BottomSheetBehavior.STATE_HIDDEN,
    val searchQuery: String = "",
    val searchFocused: Boolean = false,
    val selectedBottomNavigationViewItemId: Int = R.id.action_unchecked,
) : Parcelable
