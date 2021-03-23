package com.lookaround.ui.main.model

import android.location.Location
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.Failed
import com.lookaround.core.android.model.WithValue
import com.lookaround.ui.main.MainViewModel
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        states
            .map { it.locationState }
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }

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
val MainViewModel.bottomSheetStateUpdates: Flow<Int>
    get() = states.map { it.bottomSheetState }
