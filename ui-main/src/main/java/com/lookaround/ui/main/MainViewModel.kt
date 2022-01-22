package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import com.lookaround.core.android.architecture.FlowViewModel
import com.lookaround.core.android.ext.initialState
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
@HiltViewModel
class MainViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, processor: MainFlowProcessor) :
    FlowViewModel<MainIntent, MainState, MainSignal>(
        savedStateHandle.initialState(),
        savedStateHandle,
        processor
    )
