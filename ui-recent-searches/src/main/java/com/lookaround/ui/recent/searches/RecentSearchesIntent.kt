package com.lookaround.ui.recent.searches

sealed interface RecentSearchesIntent {
    object LoadSearches : RecentSearchesIntent
}
