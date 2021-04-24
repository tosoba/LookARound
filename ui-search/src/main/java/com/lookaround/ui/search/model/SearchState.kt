package com.lookaround.ui.search.model

import android.os.Parcelable
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.ParcelableList
import com.lookaround.core.android.model.Point
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchState(
    val points: Loadable<ParcelableList<Point>> = Empty,
    val lastPerformedWithLocationPriority: Boolean = false
) : Parcelable
