package com.lookaround.ui.main.model

import android.location.Location
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.Ready
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.model.NodeDTO
import java.util.*

object LoadingPlacesUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(markers = state.markers.copyWithLoadingInProgress)
}

data class PlacesErrorUpdate(private val throwable: Throwable) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(markers = state.markers.copyWithError(throwable))
}

data class PlacesLoadedUpdate(private val nodes: List<NodeDTO>) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(
            markers =
                Ready(
                    ParcelableSortedSet(
                        nodes.map(::Marker).toSortedSet { marker1, marker2 ->
                            val userLocation = state.locationState
                            if (userLocation !is WithValue<Location>) {
                                throw IllegalStateException("User location does not have a value.")
                            }
                            marker1
                                .location
                                .distanceTo(userLocation.value)
                                .compareTo(marker2.location.distanceTo(userLocation.value))
                        }
                    )
                )
        )
}

data class LocationLoadedUpdate(val location: Location) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(
            locationState = Ready(location),
            markers =
                if (state.markers is WithValue) {
                    state.markers.map { ParcelableSortedSet(TreeSet(it.items)) }
                } else {
                    state.markers
                }
        )
}

object LoadingLocationUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(locationState = state.locationState.copyWithLoadingInProgress)
}

object LocationDisabledUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copyWithLocationException(LocationDisabledException)
}

object FailedToUpdateLocationUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copyWithLocationException(LocationUpdateFailureException)
}
