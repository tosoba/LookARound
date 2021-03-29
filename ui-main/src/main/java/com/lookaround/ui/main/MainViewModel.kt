package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import com.lookaround.core.android.base.arch.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.main.model.MainStateUpdate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class MainViewModel
@AssistedInject
constructor(@Assisted savedStateHandle: SavedStateHandle, processor: MainFlowProcessor) :
    FlowViewModel<MainIntent, MainStateUpdate, MainState, MainSignal>(
        savedStateHandle.initialState(),
        processor,
        savedStateHandle
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<MainViewModel>
}
