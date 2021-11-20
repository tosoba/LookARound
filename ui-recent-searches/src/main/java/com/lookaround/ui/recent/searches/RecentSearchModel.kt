package com.lookaround.ui.recent.searches

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecentSearchModel(val id: Long, val label: String, val type: Type) : Parcelable {
    enum class Type {
        AROUND,
        AUTOCOMPLETE
    }
}
