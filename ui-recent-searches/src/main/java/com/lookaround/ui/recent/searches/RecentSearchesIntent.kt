package com.lookaround.ui.recent.searches

import com.lookaround.core.model.SearchType

sealed interface RecentSearchesIntent {
    data class LoadSearches(val query: String?) : RecentSearchesIntent
    data class DeleteSearch(val id: Long, val type: SearchType) : RecentSearchesIntent
}
