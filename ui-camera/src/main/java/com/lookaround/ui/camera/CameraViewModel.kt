package com.lookaround.ui.camera

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.architecture.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CameraViewModel
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    processor: CameraFlowProcessor,
) :
    FlowViewModel<CameraIntent, CameraState, CameraSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<CameraViewModel>
}
