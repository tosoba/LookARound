package com.lookaround.ui.recent.searches

sealed interface RecentSearchesIntent {
    object IncreaseLimit : RecentSearchesIntent
}
