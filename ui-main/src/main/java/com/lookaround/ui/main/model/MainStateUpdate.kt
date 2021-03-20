package com.lookaround.ui.main.model

import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
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
            state.copy(
                locationState =
                    if (state.locationState is WithValue) LoadingNext(state.locationState.value)
                    else LoadingFirst
            )
    }

    object LocationPermissionDenied : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(
                locationState = state.locationState.copyWithError(LocationPermissionDeniedException)
            )
    }

    object LocationDisabled : MainStateUpdate() {
        override fun invoke(state: MainState): MainState =
            state.copy(
                locationState =
                    if (state.locationState is WithValue) {
                        FailedNext(state.locationState.value, LocationDisabledException)
                    } else {
                        FailedFirst(LocationDisabledException)
                    }
            )
    }
}
