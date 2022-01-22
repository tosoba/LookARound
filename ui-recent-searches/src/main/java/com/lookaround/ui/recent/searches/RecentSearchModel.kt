package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Parcelable
import com.lookaround.core.model.SearchType
import java.util.*
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecentSearchModel(
    val id: Long,
    val label: String,
    val type: SearchType,
    val location: Location?,
    val lastSearchedAt: Date
) : Parcelable
