package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.base.arch.FlowViewModel
import com.lookaround.ui.main.model.MainIntent
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
constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    @Assisted initialState: MainState,
    processor: MainFlowProcessor
) :
    FlowViewModel<MainIntent, MainStateUpdate, MainState, Unit>(
        initialState,
        processor,
        savedStateHandle
    ) {
    @AssistedFactory
    interface Factory {
        fun create(
            savedStateHandle: SavedStateHandle,
            initialState: MainState = MainState()
        ): MainViewModel
    }
}
