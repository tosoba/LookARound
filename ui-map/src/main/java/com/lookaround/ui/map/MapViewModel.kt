package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.FlowViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class MapViewModel @Inject constructor(
    initialState: MapState = MapState(),
    processor: MapFlowProcessor
) : FlowViewModel<MapIntent, MapStateUpdate, MapState, MapSignal>(initialState, processor)
