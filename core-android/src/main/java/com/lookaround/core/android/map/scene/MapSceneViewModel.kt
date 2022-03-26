package com.lookaround.core.android.map.scene

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.ext.initialState
import com.lookaround.core.android.map.scene.model.MapSceneIntent
import com.lookaround.core.android.map.scene.model.MapSceneSignal
import com.lookaround.core.android.map.scene.model.MapSceneState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@HiltViewModel
class MapSceneViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, processor: MapSceneFlowProcessor) :
    FlowViewModel<MapSceneIntent, MapSceneState, MapSceneSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    )
