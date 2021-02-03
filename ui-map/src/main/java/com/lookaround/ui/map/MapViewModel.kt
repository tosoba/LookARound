package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.FlowViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class MapViewModel(
    initialState: MapState = MapState(),
    processor: MapFlowProcessor
) : FlowViewModel<MapIntent, MapStateUpdate, MapState, MapSignal>(initialState, processor)
