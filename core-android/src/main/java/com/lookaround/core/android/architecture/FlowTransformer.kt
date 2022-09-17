package com.lookaround.core.android.architecture

import kotlinx.coroutines.flow.Flow

interface FlowTransformer<STATE : Any, T : Any, SIGNAL : Any> {
    operator fun invoke(
        flow: Flow<T>,
        currentState: () -> STATE,
        signal: suspend (SIGNAL) -> Unit,
    ): Flow<STATE.() -> STATE>
}
