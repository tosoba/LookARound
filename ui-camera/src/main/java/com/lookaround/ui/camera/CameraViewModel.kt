package com.lookaround.ui.camera

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class CameraViewModel
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    @Assisted initialState: CameraState,
    processor: CameraFlowProcessor
) :
    FlowViewModel<CameraIntent, CameraStateUpdate, CameraState, CameraSignal>(
        initialState, processor, savedStateHandle) {
    @AssistedFactory
    interface Factory {
        fun create(
            savedStateHandle: SavedStateHandle,
            initialState: CameraState = CameraState()
        ): CameraViewModel
    }
}