package com.lookaround.ui.recent.searches

import android.os.Parcelable
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.ParcelableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecentSearchesState(
    val searches: Loadable<ParcelableList<RecentSearchModel>> = Empty,
) : Parcelable {
    companion object {
        internal const val SEARCHES_LIMIT_INCREMENT = 10
    }
}
