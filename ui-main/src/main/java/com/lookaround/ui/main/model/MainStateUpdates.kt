package com.lookaround.ui.main.model

import android.location.Location
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.model.*
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.model.PointDTO
import java.util.*

object LoadingSearchResultsUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(markers = state.markers.copyWithLoadingInProgress)
}

data class SearchErrorUpdate(private val exception: Exception) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(markers = state.markers.copyWithError(exception))
}

data class SearchAroundResultsLoadedUpdate(
    private val nodes: Iterable<NodeDTO>,
) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(
            markers =
                Ready(
                    ParcelableSortedSet(
                        nodes.map(::Marker).toSetSortedByDistanceToUserLocation(state.locationState)
                    )
                )
        )
}

data class AutocompleteSearchResultsLoadedUpdate(
    private val points: Iterable<PointDTO>,
) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(
            markers =
                Ready(
                    ParcelableSortedSet(
                        points
                            .map(::Marker)
                            .toSetSortedByDistanceToUserLocation(state.locationState)
                    )
                )
        )
}

object NoPlacesFoundUpdate : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState = state.copy(markers = Empty)
}

private fun List<Marker>.toSetSortedByDistanceToUserLocation(
    userLocation: Loadable<Location>
): SortedSet<Marker> = toSortedSet { marker1, marker2 ->
    if (userLocation !is WithValue<Location>) {
        throw IllegalStateException("User location does not have a value.")
    }
    marker1.location
        .distanceTo(userLocation.value)
        .compareTo(marker2.location.distanceTo(userLocation.value))
}

data class LocationLoadedUpdate(val location: Location) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState =
        state.copy(
            locationState = Ready(location),
            markers = state.markers.map { ParcelableSortedSet(TreeSet(it.items)) }
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

data class FailedToUpdateLocationUpdate(val exception: Exception) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState = state.copyWithLocationException(exception)
}

data class RecentSearchesCountUpdate(private val count: Int) : (MainState) -> MainState {
    override fun invoke(state: MainState): MainState = state.copy(recentSearchesCount = count)
}
