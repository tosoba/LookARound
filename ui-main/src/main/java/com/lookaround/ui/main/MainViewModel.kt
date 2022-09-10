package com.lookaround.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lookaround.core.android.architecture.DebugLoggingMiddleware
import com.lookaround.core.android.architecture.IMviFlowStateContainer
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
constructor(
    savedStateHandle: SavedStateHandle,
    stateContainerFactory: MainStateContainer.Factory,
) :
    ViewModel(),
    IMviFlowStateContainer<MainState, MainIntent, MainSignal> by stateContainerFactory.create(
        savedStateHandle
    ) {

    init {
        launch(
            viewModelScope,
            updateMiddlewares = listOf(DebugLoggingMiddleware("STATE_UPDATE")),
            stateMiddlewares = listOf(DebugLoggingMiddleware("NEW_STATE"))
        )
    }
}
