package com.lookaround.ui.camera

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@HiltViewModel
class CameraViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, processor: CameraFlowProcessor) :
    FlowViewModel<CameraIntent, CameraState, CameraSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    )
