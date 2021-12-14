package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.model.IPlaceType

sealed interface MainIntent {
    data class GetPlacesOfType(val type: IPlaceType) : MainIntent

    object LocationPermissionGranted : MainIntent

    object LocationPermissionDenied : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            state.copyWithLocationException(LocationPermissionDeniedException)
    }

    data class LiveBottomSheetStateChanged(
        @BottomSheetBehavior.State private val state: Int,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(mainState: MainState): MainState =
            mainState.copy(lastLiveBottomSheetState = state)
    }

    data class SearchModeChanged(
        private val mode: MainSearchMode,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            MainState(searchMode = mode, searchFocused = false)
    }

    data class SearchQueryChanged(val query: String) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            when (state.searchMode) {
                MainSearchMode.AUTOCOMPLETE -> state.copy(autocompleteSearchQuery = query)
                MainSearchMode.PLACE_TYPES -> state.copy(placeTypesSearchQuery = query)
                MainSearchMode.PLACE_LIST -> state.copy(placeListSearchQuery = query)
                MainSearchMode.RECENT -> state.copy(recentSearchQuery = query)
            }
    }

    data class SearchFocusChanged(
        private val focused: Boolean,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            if (state.searchFocused == focused) state else state.copy(searchFocused = focused)
    }

    data class BottomNavigationViewItemSelected(
        private val itemId: Int,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            if (state.selectedBottomNavigationViewItemId == itemId) state
            else state.copy(selectedBottomNavigationViewItemId = itemId)
    }

    data class LoadSearchAroundResults(val searchId: Long) : MainIntent

    data class LoadSearchAutocompleteResults(val searchId: Long) : MainIntent
}
