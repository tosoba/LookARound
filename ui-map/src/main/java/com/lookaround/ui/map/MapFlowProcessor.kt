package com.lookaround.ui.map

import com.lookaround.core.android.base.arch.FlowProcessor
import com.lookaround.core.usecase.IsConnectedFlow
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class MapFlowProcessor
@Inject
constructor(
    private val isConnectedFlow: IsConnectedFlow,
) : FlowProcessor<MapIntent, MapStateUpdate, MapState, MapSignal> {
    override fun updates(
        coroutineScope: CoroutineScope,
        intents: Flow<MapIntent>,
        currentState: () -> MapState,
        states: Flow<MapState>,
        intent: suspend (MapIntent) -> Unit,
        signal: suspend (MapSignal) -> Unit
    ): Flow<MapStateUpdate> =
        merge(
            intents.filterIsInstance<MapIntent.LoadingScene>().transformLatest {
                emit(MapStateUpdate.LoadingScene(it.scene))
                delay(SCENE_LOADING_TIME_LIMIT_MS)
                if (!currentState().sceneLoaded) emit(MapStateUpdate.SceneLoadingTimeoutOccurred)
            },
            intents.filterIsInstance<MapIntent.SceneLoaded>().map { MapStateUpdate.SceneLoaded },
        )

    override fun sideEffects(
        coroutineScope: CoroutineScope,
        currentState: () -> MapState,
        states: Flow<MapState>,
        signal: suspend (MapSignal) -> Unit
    ) {
        states
            .filter { it.sceneLoadingTimeoutOccurred }
            .combine(isConnectedFlow().filter { it }) { state, _ ->
                signal(MapSignal.RetryLoadScene(state.scene))
            }
            .launchIn(coroutineScope)
    }

    companion object {
        private const val SCENE_LOADING_TIME_LIMIT_MS: Long = 5_000L
    }
}
