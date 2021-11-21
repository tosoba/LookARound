package com.lookaround.ui.main.model

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.model.IPlaceType

sealed interface MainIntent {
    data class LoadPlaces(val type: IPlaceType) : MainIntent

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

    data class SearchQueryChanged(
        private val query: String,
    ) : MainIntent, (MainState) -> MainState {
        override fun invoke(state: MainState): MainState =
            if (state.searchQuery == query) state else state.copy(searchQuery = query)
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
}
