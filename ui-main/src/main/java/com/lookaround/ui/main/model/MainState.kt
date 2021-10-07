package com.lookaround.ui.main.model

import android.location.Location
import android.os.Parcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.model.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(
    val markers: Loadable<ParcelableList<Marker>> = Ready(ParcelableList(SampleMarkers.get())),
    val locationState: Loadable<Location> = Empty,
    val bottomSheetState: BottomSheetState =
        BottomSheetState(BottomSheetBehavior.STATE_HIDDEN, false),
    val searchQuery: String = "",
    val searchFocused: Boolean = false
) : Parcelable
