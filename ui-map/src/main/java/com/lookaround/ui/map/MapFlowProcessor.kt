package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.FlowProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MapFlowProcessor : FlowProcessor<MapIntent, MapStateUpdate, MapState, MapSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MapIntent>,
        currentState: () -> MapState,
        states: Flow<MapState>,
        intent: suspend (MapIntent) -> Unit,
        signal: suspend (MapSignal) -> Unit
    ): Flow<MapStateUpdate> = emptyFlow()
}