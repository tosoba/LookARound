package com.lookaround

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import com.lookaround.ui.place.list.PlaceListFragment
import com.lookaround.ui.place.types.PlaceTypesFragment
import com.lookaround.ui.recent.searches.RecentSearchesFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
internal enum class MainFragmentFactory(val newInstance: () -> Fragment) {
    PLACE_TYPES({ PlaceTypesFragment() }),
    PLACE_LIST({ PlaceListFragment() }),
    RECENT_SEARCHES({ RecentSearchesFragment() })
}
