package com.lookaround.ui.main.model

import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.*
import com.lookaround.core.model.NodeDTO

sealed class MainStateUpdate : StateUpdate<MainState> {
    object LoadingPlaces : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(markers = state.markers.copyWithLoadingInProgress)
    }

    data class PlacesError(private val throwable: Throwable) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(markers = state.markers.copyWithError(throwable))
    }

    data class PlacesLoaded(val nodes: List<NodeDTO>) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(
                markers =
                    if (state.markers is WithValue) Ready(state.markers.value + nodes.map(::Marker))
                    else Ready(ParcelableList(nodes.map(::Marker)))
            )
    }

    data class LocationLoaded(val location: android.location.Location) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(locationState = Ready(location))
    }

    object LoadingLocation : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(locationState = state.locationState.copyWithLoadingInProgress)
    }

    object LocationPermissionDenied : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copyWithLocationException(LocationPermissionDeniedException)
    }

    object LocationDisabled : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copyWithLocationException(LocationDisabledException)
    }

    object FailedToUpdateLocation : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copyWithLocationException(LocationUpdateFailureException)
    }

    data class BottomSheetStateChanged(val sheetState: BottomSheetState) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState = state.copy(bottomSheetState = sheetState)
    }

    data class BottomSheetSlideChanged(val slideOffset: Float) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(bottomSheetSlideOffset = slideOffset)
    }

    data class SearchQueryChanged(val query: String) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            if (state.searchQuery == query) state else state.copy(searchQuery = query)
    }

    data class SearchFocusChanged(val focused: Boolean) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            if (state.searchFocused == focused) state else state.copy(searchFocused = focused)
    }

    protected fun MainState.copyWithLocationException(throwable: Throwable): MainState =
        copy(locationState = locationState.copyWithError(throwable))
}
