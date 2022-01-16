package com.lookaround.ui.recent.searches

sealed interface RecentSearchesIntent {
    data class LoadSearches(val query: String?) : RecentSearchesIntent
}
