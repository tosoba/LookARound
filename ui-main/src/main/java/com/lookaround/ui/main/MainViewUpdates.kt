package com.lookaround.ui.main

import android.location.Location
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.*
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationUpdateFailureUpdates: Flow<Unit>
    get() =
        states
            .map { it.locationState }
            .filter { (it as? Failed)?.error is LocationUpdateFailureException }
            .map {}

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.bottomSheetStateUpdates: Flow<BottomSheetState>
    get() = states.map { it.bottomSheetState }

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.searchFragmentVisibilityUpdates: Flow<Boolean>
    get() =
        states
            .map { (_, _, bottomSheetState, _, searchFocused) ->
                val (wrappedState, _) = bottomSheetState
                wrappedState != BottomSheetBehavior.STATE_EXPANDED &&
                    wrappedState != BottomSheetBehavior.STATE_DRAGGING &&
                    searchFocused
            }
            .debounce(500L)
            .distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.unableToLoadPlacesWithoutLocationSignals:
    Flow<MainSignal.UnableToLoadPlacesWithoutLocation>
    get() = signals.filterIsInstance()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.markerUpdates: Flow<List<Marker>>
    get() =
        states
            .map(MainState::markers::get)
            .distinctUntilChanged()
            .filterIsInstance<WithValue<ParcelableList<Marker>>>()
            .map { it.value.items }

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.placesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.markers is WithValue }.distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        states
            .map { it.locationState }
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }
