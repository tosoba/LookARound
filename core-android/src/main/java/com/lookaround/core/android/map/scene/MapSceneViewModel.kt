package com.lookaround.core.android.map.scene

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import com.lookaround.core.android.base.arch.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.map.scene.model.MapSceneState
import com.lookaround.core.android.map.scene.model.MapSceneStateUpdate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class MapSceneViewModel
@AssistedInject
constructor(@Assisted savedStateHandle: SavedStateHandle, processor: MapSceneFlowProcessor) :
    FlowViewModel<MapSceneIntent, MapSceneStateUpdate, MapSceneState, MapSceneSignal>(
        savedStateHandle.initialState(),
        processor,
        savedStateHandle
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<MapSceneViewModel>
}
