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
        @BottomSheetBehavior.State private val bottomSheetState: Int,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            state.copy(lastLiveBottomSheetState = bottomSheetState)
    }

    data class SearchQueryChanged(val query: String) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            state.copy(autocompleteSearchQuery = query)
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
