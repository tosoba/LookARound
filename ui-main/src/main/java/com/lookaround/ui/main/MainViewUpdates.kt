package com.lookaround.ui.main

import android.location.Location
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.*
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import java.util.*
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationUpdateFailureUpdates: Flow<Unit>
    get() =
        states
            .map(MainState::locationState::get)
            .filter { (it as? Failed)?.error is LocationUpdateFailureException }
            .map {}

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.bottomSheetStateUpdates: Flow<Int>
    get() =
        signals
            .filterIsInstance<MainSignal.BottomSheetStateChanged>()
            .map(MainSignal.BottomSheetStateChanged::state::get)

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.searchFragmentVisibilityUpdates: Flow<Boolean>
    get() =
        states
            .map(MainState::searchFocused::get)
            .combine(
                signals
                    .filterIsInstance<MainSignal.BottomSheetStateChanged>()
                    .map(MainSignal.BottomSheetStateChanged::state::get)
            ) { searchFocused, bottomSheetState ->
                bottomSheetState != BottomSheetBehavior.STATE_EXPANDED &&
                    bottomSheetState != BottomSheetBehavior.STATE_DRAGGING &&
                    searchFocused
            }
            .debounce(500L)
            .distinctUntilChanged()

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.unableToLoadPlacesWithoutLocationSignals:
    Flow<MainSignal.UnableToLoadPlacesWithoutLocation>
    get() = signals.filterIsInstance()

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.placesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.markers is WithValue }.distinctUntilChanged()

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        states
            .map(MainState::locationState::get)
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }

@ExperimentalTime
@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.markerUpdates: Flow<Loadable<ParcelableSortedSet<Marker>>>
    get() = states.map(MainState::markers::get).distinctUntilChanged()
