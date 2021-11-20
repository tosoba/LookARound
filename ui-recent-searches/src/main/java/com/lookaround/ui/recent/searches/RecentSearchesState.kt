package com.lookaround.ui.recent.searches

import android.os.Parcelable
import com.lookaround.core.android.model.Empty
import com.lookaround.core.android.model.Loadable
import com.lookaround.core.android.model.ParcelableList
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecentSearchesState(
    val searches: Loadable<ParcelableList<RecentSearchModel>> = Empty,
    val totalSearchesCount: Int = 0,
    val currentLimit: Int = INITIAL_SEARCHES_LIMIT
) : Parcelable {
    companion object {
        internal const val INITIAL_SEARCHES_LIMIT = 10
        internal const val SEARCHES_LIMIT_INCREMENT = 10
    }
}
