package com.lookaround.ui.main.model

import android.location.Location
import android.os.Parcelable
import biz.laenger.android.vpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.model.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(
    val markers: Loadable<ParcelableList<Marker>> = Empty,
    val locationState: Loadable<Location> = Empty,
    val bottomSheetState: BottomSheetState =
        BottomSheetState(ViewPagerBottomSheetBehavior.STATE_HIDDEN, false),
    val searchQuery: String = "",
    val searchFocused: Boolean = false
) : Parcelable
