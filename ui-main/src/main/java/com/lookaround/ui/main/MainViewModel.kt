package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.architecture.SavedStateViewModelFactory
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class MainViewModel
@AssistedInject
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    processor: MainFlowProcessor,
) :
    FlowViewModel<MainIntent, MainState, MainSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    ) {
    @AssistedFactory interface Factory : SavedStateViewModelFactory<MainViewModel>
}
