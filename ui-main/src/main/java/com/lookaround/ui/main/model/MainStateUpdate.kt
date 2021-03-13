package com.lookaround.ui.main.model

import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableList
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
                    if (state.markers is WithValue) Ready(state.markers.value + nodes.map(::Marker))
                    else Ready(ParcelableList(nodes.map(::Marker)))
            )
    }
}
