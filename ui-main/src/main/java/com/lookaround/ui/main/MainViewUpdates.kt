package com.lookaround.ui.main

import android.location.Location
import com.lookaround.core.android.exception.LocationUpdateFailureException
import com.lookaround.core.android.model.*
import com.lookaround.ui.main.model.MainState
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationUpdateFailureUpdates: Flow<Unit>
    get() =
        mapStates(MainState::locationState)
            .filter { (it as? Failed)?.error is LocationUpdateFailureException }
            .map {}

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.placesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.markers is WithValue }.distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.recentSearchesBottomNavItemVisibilityUpdates: Flow<Boolean>
    get() = states.map { it.recentSearchesCount > 0 }.distinctUntilChanged()

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.locationReadyUpdates: Flow<Location>
    get() =
        mapStates(MainState::locationState)
            .filterIsInstance<WithValue<Location>>()
            .map(WithValue<Location>::value::get)
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }

@FlowPreview
@ExperimentalCoroutinesApi
val MainViewModel.markerUpdates: Flow<Loadable<ParcelableSortedSet<Marker>>>
    get() = mapStates(MainState::markers).distinctUntilChanged()
