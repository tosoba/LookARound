package com.lookaround.core.android.architecture

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface FlowProcessor<Intent : Any, State : Any, Signal : Any> {
    fun updates(
        intents: Flow<Intent>,
        currentState: () -> State,
        signal: suspend (Signal) -> Unit
    ): Flow<(State) -> State>

    fun sideEffects(
        coroutineScope: CoroutineScope,
        intents: Flow<Intent>,
        states: Flow<State>,
        currentState: () -> State,
        signal: suspend (Signal) -> Unit
    ) = Unit

    fun stateWillUpdate(
        currentState: State,
        nextState: State,
        update: (State) -> State,
        savedStateHandle: SavedStateHandle
    ) = Unit
}
