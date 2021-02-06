package com.lookaround.ui.map

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class MapViewModel @AssistedInject constructor(
    @Assisted initialState: MapState,
    @Assisted savedStateHandle: SavedStateHandle,
    processor: MapFlowProcessor
) : FlowViewModel<MapIntent, MapStateUpdate, MapState, MapSignal>(
    initialState, processor, savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(initialState: MapState, savedStateHandle: SavedStateHandle): MapViewModel
    }
}
