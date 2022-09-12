package com.lookaround.core.android.architecture

import kotlinx.coroutines.flow.Flow

interface IntentHandler<STATE : Any, INTENT : Any, SIGNAL : Any> {
    operator fun invoke(
        intents: Flow<INTENT>,
        currentState: () -> STATE,
        signal: suspend (SIGNAL) -> Unit,
    ): Flow<STATE.() -> STATE>
}
