package com.lookaround.core.android.architecture

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface FlowProcessor<Intent : Any, State : Any, Signal : Any> {
    fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<Intent>,
        currentState: () -> State,
        states: Flow<State>,
        intent: suspend (Intent) -> Unit,
        signal: suspend (Signal) -> Unit
    ): Flow<(State) -> State>

    fun sideEffects(
        coroutineScope: CoroutineScope,
        intents: Flow<Intent>,
        currentState: () -> State,
        states: Flow<State>,
        signal: suspend (Signal) -> Unit
    ) = Unit

    fun stateWillUpdate(
        currentState: State,
        nextState: State,
        update: (State) -> State,
        savedStateHandle: SavedStateHandle
    ) = Unit
}
