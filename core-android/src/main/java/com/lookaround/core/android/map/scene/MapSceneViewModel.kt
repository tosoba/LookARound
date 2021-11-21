package com.lookaround.core.android.map.scene

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.architecture.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.map.scene.model.MapSceneState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class MapSceneViewModel
@AssistedInject
constructor(@Assisted savedStateHandle: SavedStateHandle, processor: MapSceneFlowProcessor) :
    FlowViewModel<MapSceneIntent, MapSceneState, MapSceneSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<MapSceneViewModel>
}
