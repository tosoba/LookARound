package com.lookaround.ui.main

import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.*
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
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
val MainViewModel.searchFocusUpdates: Flow<Boolean>
    get() = states.map { it.searchFocused }.debounce(500L).distinctUntilChanged()

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
