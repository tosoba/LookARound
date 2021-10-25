package com.lookaround.ui.main.model

import android.location.Location
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.Ready
import com.lookaround.core.android.model.WithValue
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
                    if (state.markers is WithValue) {
                        Ready(state.markers.value + nodes.map(::Marker))
                    } else {
                        Ready(
                            ParcelableSortedSet(
                                nodes.map(::Marker).toSortedSet { marker1, marker2 ->
                                    val userLocation = state.locationState
                                    if (userLocation !is WithValue<Location>) {
                                        throw IllegalStateException(
                                            "User location does not have a value."
                                        )
                                    }
                                    marker1
                                        .location
                                        .distanceTo(userLocation.value)
                                        .compareTo(marker2.location.distanceTo(userLocation.value))
                                }
                            )
                        )
                    }
            )
    }

    data class LocationLoaded(val location: Location) : MainStateUpdate() {
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

    data class LiveBottomSheetStateChanged(
        @BottomSheetBehavior.State val sheetState: Int,
    ) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(lastLiveBottomSheetState = sheetState)
    }

    data class SearchQueryChanged(val query: String) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            if (state.searchQuery == query) state else state.copy(searchQuery = query)
    }

    data class SearchFocusChanged(val focused: Boolean) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            if (state.searchFocused == focused) state else state.copy(searchFocused = focused)
    }

    data class BottomNavigationViewItemSelected(val itemId: Int) : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            if (state.selectedBottomNavigationViewItemId == itemId) state
            else state.copy(selectedBottomNavigationViewItemId = itemId)
    }

    protected fun MainState.copyWithLocationException(throwable: Throwable): MainState =
        copy(locationState = locationState.copyWithError(throwable))
}
