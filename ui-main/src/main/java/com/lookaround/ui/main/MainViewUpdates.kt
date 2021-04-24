package com.lookaround.ui.main

import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.BottomSheetState
import com.lookaround.core.android.model.Failed
import com.lookaround.ui.main.model.MainSignal
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
val MainViewModel.searchFocusUpdates: Flow<Boolean>
    get() = states.map { it.searchFocused }.debounce(500L).distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.unableToLoadPlacesWithoutLocationSignals:
    Flow<MainSignal.UnableToLoadPlacesWithoutLocation>
    get() = signals.filterIsInstance()
