package com.lookaround.ui.recent.searches

import android.location.Location
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecentSearchModel(
    val id: Long,
    val label: String,
    val type: Type,
    val location: Location?
) : Parcelable {
    enum class Type {
        AROUND,
        AUTOCOMPLETE
    }
}
