package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.FlowProcessor
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

@ViewModelScoped
class MapFlowProcessor @Inject constructor() :
    FlowProcessor<MapIntent, MapStateUpdate, MapState, MapSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MapIntent>,
        currentState: () -> MapState,
        states: Flow<MapState>,
        intent: suspend (MapIntent) -> Unit,
        signal: suspend (MapSignal) -> Unit
    ): Flow<MapStateUpdate> = emptyFlow()
}